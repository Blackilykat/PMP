/*
 * Copyright (C) 2025 Blackilykat and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat.pmp.client;

import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.util.OverridingSingleThreadExecutor;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.Picture;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Player {
	public static final EventSource<Boolean> EVENT_PLAY_PAUSE = new EventSource<>();
	public static final EventSource<TrackChangeEvent> EVENT_TRACK_CHANGE = new EventSource<>();
	public static final EventSource<Long> EVENT_PROGRESS = new EventSource<>();

	private static final Logger LOGGER = LogManager.getLogger(Player.class);

	private static final AudioFormat PLAYBACK_AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100,
			16,
			2, 4, 44100, true);

	/**
	 * Buffer size when playing the already loaded audio. Too high values can make pausing unresponsive. Too low values
	 * can make audio choppy in some circumstances.
	 */
	private static final int PLAYBACK_BUFFER_SIZE = 17600;
	/**
	 * Buffer size when loading track from disk. Too low values can increase CPU usage. Too high values can make it
	 * take
	 * longer to start playing.
	 */
	private static final int LOADING_BUFFER_SIZE =
			(int) PLAYBACK_AUDIO_FORMAT.getSampleRate() * PLAYBACK_AUDIO_FORMAT.getChannels();
	/**
	 * By how much playback can be behind where it should be in ms. Too low values can make audio choppy. Too high
	 * values can make the position shown on the playbar and the actual audio position inconsistent.
	 */
	private static final int MAX_PLAYBACK_OFFSET_MS = 100;

	private static final OverridingSingleThreadExecutor DECODING_EXECUTOR = new OverridingSingleThreadExecutor();
	private static final OverridingSingleThreadExecutor LOADING_EXECUTOR = new OverridingSingleThreadExecutor();
	private static final Pair<ScheduledExecutorService, ScheduledFuture<?>> PROGRESS_EXECUTOR = new Pair<>(
			Executors.newSingleThreadScheduledExecutor(), null);
	private static final AtomicBoolean paused = new AtomicBoolean(true);
	private static boolean shouldSeek = false;
	private static Thread audioThread = null;
	private static Track currentTrack = null;
	private static byte[] pcm = null;
	private static byte[] pcmBuffer = new byte[PLAYBACK_BUFFER_SIZE];
	/**
	 * Timestamp used to keep track of the current playback position when playing.
	 *
	 * @see #getPosition()
	 * @see #setPosition(long)
	 * @see #seek(long)
	 */
	private static Instant playbackEpoch = null;
	/**
	 * Current playback position when paused.
	 *
	 * @see #getPosition()
	 * @see #setPosition(long)
	 * @see #seek(long)
	 */
	private static long playbackPosition = 0;


	public static void play() {
		maybeStart();
		synchronized(paused) {
			if(paused.get()) {
				playbackEpoch = Instant.now().minus(playbackPosition, ChronoUnit.MILLIS);
			}
			paused.set(false);
			paused.notifyAll();
		}
		PROGRESS_EXECUTOR.value = PROGRESS_EXECUTOR.key.scheduleAtFixedRate(() -> {
			EVENT_PROGRESS.call(getPosition());
		}, 50, 50, TimeUnit.MILLISECONDS);
		EVENT_PLAY_PAUSE.call(false);
	}

	public static void pause() {
		maybeStart();
		synchronized(paused) {
			if(!paused.get()) {
				playbackPosition =
						playbackEpoch == null ? 0 : Instant.now().toEpochMilli() - playbackEpoch.toEpochMilli();
			}
			paused.set(true);
			paused.notifyAll();
		}
		if(PROGRESS_EXECUTOR.value != null) {
			PROGRESS_EXECUTOR.value.cancel(false);
		}
		EVENT_PLAY_PAUSE.call(true);
	}

	public static void playPause() {
		if(paused.get()) {
			play();
		} else {
			pause();
		}
	}

	public static void play(Track track) {
		maybeStart();
		pause();
		currentTrack = track;
		LOGGER.info("Playing {}", track.getTitle());

		CompletableFuture<byte[]> albumArtFuture = new CompletableFuture<>();
		load(track, albumArtFuture);
		shouldSeek = true;
		setPosition(0);

		play();

		EVENT_TRACK_CHANGE.call(new TrackChangeEvent(track, albumArtFuture));
	}

	/**
	 * @return the current playback position in milliseconds
	 */
	public static long getPosition() {
		if(paused.get()) {
			return playbackPosition;
		}

		Instant now = Instant.now();
		return now.toEpochMilli() - playbackEpoch.toEpochMilli();
	}

	private static void setPosition(long ms) {
		if(paused.get()) {
			playbackPosition = ms;
			return;
		}

		Instant now = Instant.now();
		playbackEpoch = now.minus(ms, ChronoUnit.MILLIS);
	}

	public static void seek(long ms) {
		shouldSeek = true;

		setPosition(ms);
		EVENT_PROGRESS.call(ms);
	}

	public static Track getTrack() {
		return currentTrack;
	}

	private static void load(Track track, CompletableFuture<byte[]> albumArtFuture) {
		maybeStart();

		//noinspection resource
		PipedInputStream pipeIn = new PipedInputStream();
		PipedOutputStream pipeOut = new PipedOutputStream();
		try {
			pipeIn.connect(pipeOut);
		} catch(IOException ignored) {
			// unreachable
		}
		DECODING_EXECUTOR.submit(() -> {
			try {
				FLACDecoder decoder = new FLACDecoder(new FileInputStream(track.getFile()));
				decoder.addPCMProcessor(new PCMProcessor() {
					int processed = 0;

					@Override
					public void processStreamInfo(StreamInfo streamInfo) {
						// These are inter-channel samples, meaning they're really just frames
						pcm = new byte[(int) streamInfo.getTotalSamples() * PLAYBACK_AUDIO_FORMAT.getFrameSize()];
					}

					@Override
					public void processPCM(ByteData byteData) {
						try {
							pipeOut.write(byteData.getData(), 0, byteData.getLen());
						} catch(IOException e) {
							throw new RuntimeException(e);
						}
					}
				});

				try {
					for(Metadata metadatum : decoder.readMetadata()) {
						if(!(metadatum instanceof Picture picture)) {
							continue;
						}
						if(picture.getPictureType() != Picture.PictureType.Cover_front) {
							continue;
						}

						albumArtFuture.complete(picture.getImage());
					}
					if(!albumArtFuture.isDone()) {
						albumArtFuture.complete(null);
					}

					decoder.decodeFrames();
				} catch(RuntimeException e) {
					if(e.getCause() instanceof InterruptedException) {
						return;
					}
					throw e;
				} catch(IOException e) {
					LOGGER.error("Error ", e);
					if(!albumArtFuture.isDone()) {
						albumArtFuture.completeExceptionally(e);
					}
					throw new RuntimeException(e);
				} catch(Exception e) {
					LOGGER.error("Unknown exception", e);
				}

				LOGGER.info("Done decoding");
			} catch(IOException e) {
				LOGGER.error("Could not decode", e);
			}
		});

		LOADING_EXECUTOR.submit(() -> {

			AudioInputStream original = new AudioInputStream(pipeIn, track.getAudioFormat(),
					track.getStreamInfo().getTotalSamples());
			AudioInputStream converted = AudioSystem.getAudioInputStream(PLAYBACK_AUDIO_FORMAT, original);

			try {
				int processed = 0;
				byte[] buffer = new byte[LOADING_BUFFER_SIZE];
				while(processed < pcm.length) {
					int read = converted.read(buffer);

					System.arraycopy(buffer, 0, pcm, processed, read);
					processed += read;
				}
			} catch(InterruptedIOException ignored) {
			} catch(IOException e) {
				LOGGER.error("Unexpected IOException", e);
			}
		});
	}

	private static void maybeStart() {
		if(audioThread != null) {
			return;
		}
		audioThread = new Thread("Audio") {
			@Override
			public void run() {
				Line.Info info = new DataLine.Info(SourceDataLine.class, PLAYBACK_AUDIO_FORMAT, PLAYBACK_BUFFER_SIZE);
				SourceDataLine line;

				try {
					line = (SourceDataLine) AudioSystem.getLine(info);
					line.open(PLAYBACK_AUDIO_FORMAT, PLAYBACK_BUFFER_SIZE);
					line.start();
				} catch(LineUnavailableException e) {
					LOGGER.error("Line unavailable (cannot play audio)", e);
					return;
				}

				int framePosition;

				try {
					//noinspection InfiniteLoopStatement
					while(true) {
						synchronized(paused) {
							if(paused.get()) {
								paused.wait();
							}
						}

						// sample size is in bits (/ 8) and position is in milliseconds (/1000)
						framePosition = ((int) (
								(PLAYBACK_AUDIO_FORMAT.getFrameRate() * PLAYBACK_AUDIO_FORMAT.getSampleSizeInBits()
										* PLAYBACK_AUDIO_FORMAT.getChannels() * getPosition()) / 8000));
						framePosition -= framePosition % PLAYBACK_AUDIO_FORMAT.getFrameSize();

						while(!paused.get() && !shouldSeek) {


							int maxOffset = (int) (MAX_PLAYBACK_OFFSET_MS * PLAYBACK_AUDIO_FORMAT.getFrameRate()
									/ 1000);

							int bottomExpectedPos = ((int) (
									(PLAYBACK_AUDIO_FORMAT.getFrameRate() * PLAYBACK_AUDIO_FORMAT.getSampleSizeInBits()
											* PLAYBACK_AUDIO_FORMAT.getChannels() * getPosition()) / 8000));
							bottomExpectedPos -= bottomExpectedPos % PLAYBACK_AUDIO_FORMAT.getFrameSize();

							int topExpectedPos = bottomExpectedPos + PLAYBACK_BUFFER_SIZE;

							int minPosDiff = Math.min(Math.abs(framePosition - bottomExpectedPos),
									Math.abs(framePosition - topExpectedPos));
							if(framePosition > bottomExpectedPos && framePosition < topExpectedPos) {
								minPosDiff = 0;
							}

							if(minPosDiff > maxOffset) {
								LOGGER.warn("Correcting frame position: was {}, is {}, {}ms offset", framePosition,
										topExpectedPos, minPosDiff / PLAYBACK_AUDIO_FORMAT.getFrameRate() * 1000);
								framePosition = topExpectedPos;
							}

							if(framePosition > pcm.length) {
								pause();
								break;
							}

							if(framePosition + PLAYBACK_BUFFER_SIZE > pcm.length) {
								line.write(pcm, framePosition, pcm.length - framePosition);

								pause();
								break;
							}

							//noinspection ManualArrayCopy
							for(int i = 0; i < PLAYBACK_BUFFER_SIZE; i++) {
								pcmBuffer[i] = pcm[framePosition + i];
							}

							line.write(pcmBuffer, 0, PLAYBACK_BUFFER_SIZE);

							framePosition += PLAYBACK_BUFFER_SIZE;
						}
						shouldSeek = false;
					}
				} catch(InterruptedException e) {
					LOGGER.warn("Stopping audio thread due to interrupt");
				}
			}
		};
		audioThread.start();
	}

	public record TrackChangeEvent(Track track, CompletableFuture<byte[]> picture) {}
}
