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

import dev.blackilykat.jpasimple.PASimple;
import dev.blackilykat.jpasimple.PulseAudioException;
import dev.blackilykat.jpasimple.SampleFormat;
import dev.blackilykat.jpasimple.SampleSpec;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.OperationNotSupportedException;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Player {
	public static final EventSource<Boolean> EVENT_PLAY_PAUSE = new EventSource<>();
	public static final EventSource<TrackChangeEvent> EVENT_TRACK_CHANGE = new EventSource<>();
	public static final EventSource<Long> EVENT_PROGRESS = new EventSource<>();
	public static final EventSource<CurrentTrackLoadEvent> EVENT_CURRENT_TRACK_LOAD = new EventSource<>();

	private static final Logger LOGGER = LogManager.getLogger(Player.class);

	private static final AudioFormat PLAYBACK_AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100,
			16,
			2, 4, 44100, false);

	/**
	 * Buffer size when playing the already loaded audio. Too high values can make pausing unresponsive. Too low values
	 * can make audio choppy in some circumstances.
	 */
	private static final int PLAYBACK_BUFFER_SIZE = 8800;
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

		shouldSeek = true;
		setPosition(0);

		CompletableFuture<byte[]> albumArtFuture = new CompletableFuture<>();
		CompletableFuture<byte[]> pcmDataFuture = new CompletableFuture<>();

		pcmDataFuture.thenAccept(pcmData -> {
			pcm = pcmData;
		});

		AtomicBoolean firstLoad = new AtomicBoolean(true);
		Consumer<Integer> onPcmLoad = bytes -> {
			if(bytes >= PLAYBACK_AUDIO_FORMAT.getSampleRate() * PLAYBACK_AUDIO_FORMAT.getChannels()
					&& firstLoad.getAndSet(false)) {
				play();
			}
			EVENT_CURRENT_TRACK_LOAD.call(new CurrentTrackLoadEvent(bytes, pcm.length));
		};

		load(track, albumArtFuture, pcmDataFuture, onPcmLoad);

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


	/**
	 * Loads the specified track's album art and PCM data in the playback audio format.
	 *
	 * @param track The track to load
	 * @param albumArtFuture A future which will get completed once the entirety of the album art is loaded. The album
	 * art is returned as it is found in the track's metadata.
	 * @param pcmDataFuture A future which will get completed as soon as the PCM data array is created. It will
	 * immediately be empty. If this is null, PCM data won't be loaded at all.
	 * @param onPcmLoad A consumer which will be called each time a chunk of the PCM data is loaded. It accepts the
	 * amount of bytes of the pcm data read.
	 */
	private static void load(@Nonnull Track track, @Nullable CompletableFuture<byte[]> albumArtFuture,
			@Nullable CompletableFuture<byte[]> pcmDataFuture, @Nullable Consumer<Integer> onPcmLoad) {
		maybeStart();

		//noinspection resource
		PipedInputStream pipeIn = new PipedInputStream();
		PipedOutputStream pipeOut = new PipedOutputStream();
		try {
			pipeIn.connect(pipeOut);
		} catch(IOException ignored) {
			// unreachable
		}

		final AtomicReference<byte[]> pcmData = new AtomicReference<>(null);

		DECODING_EXECUTOR.submit(() -> {
			try {
				FLACDecoder decoder = new FLACDecoder(new FileInputStream(track.getFile()));
				decoder.addPCMProcessor(new PCMProcessor() {
					int processed = 0;

					@Override
					public void processStreamInfo(StreamInfo streamInfo) {
						if(pcmDataFuture != null) {
							LOGGER.debug("Streaminfo: {}", streamInfo);
							// These are inter-channel samples, meaning they're really just frames
							int size = (int) (streamInfo.getTotalSamples() * PLAYBACK_AUDIO_FORMAT.getFrameSize()
									* PLAYBACK_AUDIO_FORMAT.getSampleRate() / streamInfo.getSampleRate());

							size -= size % PLAYBACK_AUDIO_FORMAT.getFrameSize();

							synchronized(pcmData) {
								pcmData.set(new byte[size]);
								pcmData.notifyAll();
							}

							pcmDataFuture.complete(pcmData.get());
						}
					}

					@Override
					public void processPCM(ByteData byteData) {
						assert pcmDataFuture != null;
						try {
							pipeOut.write(byteData.getData(), 0, byteData.getLen());
						} catch(IOException e) {
							throw new RuntimeException(e);
						}
					}
				});

				try {
					for(Metadata metadatum : decoder.readMetadata()) {
						if(albumArtFuture == null) {
							continue;
						}
						if(!(metadatum instanceof Picture picture)) {
							continue;
						}
						if(picture.getPictureType() != Picture.PictureType.Cover_front) {
							continue;
						}

						albumArtFuture.complete(picture.getImage());
					}
					if(albumArtFuture != null && !albumArtFuture.isDone()) {
						albumArtFuture.complete(null);
					}

					if(pcmDataFuture != null) {
						decoder.decodeFrames();
					}
				} catch(RuntimeException e) {
					if(e.getCause() instanceof InterruptedException) {
						return;
					}
					throw e;
				} catch(IOException e) {
					LOGGER.error("Error ", e);
					if(albumArtFuture != null && !albumArtFuture.isDone()) {
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


		if(pcmDataFuture != null) {
			LOADING_EXECUTOR.submit(() -> {
				try {
					synchronized(pcmData) {
						while(pcmData.get() == null) {
							pcmData.wait();
						}
					}

					byte[] pcm = pcmData.get();

					AudioInputStream original = new AudioInputStream(pipeIn, track.getAudioFormat(),
							track.getStreamInfo().getTotalSamples());
					AudioInputStream converted = AudioSystem.getAudioInputStream(PLAYBACK_AUDIO_FORMAT, original);
					int processed = 0;
					byte[] buffer = new byte[LOADING_BUFFER_SIZE];

					while(processed < pcm.length) {
						int read = converted.read(buffer);

						// It is unclear why, but sometimes reading from converted gives an amount of bytes different
						// from
						// what was calculated from streaminfo. The difference is always somewhat small, but got up to
						// over half a second in one case. From testing, the most accurate seems to be the calculated
						// pcm.length.
						if(read == -1) {
							LOGGER.debug("Stream/array disagreement (processed: {}, array size: {})", processed,
									pcm.length);
							break;
						}

						if(processed + read > pcm.length) {
							LOGGER.debug("Stream/array disagreement (skipped {} bytes)", read);
							break;
						}

						System.arraycopy(buffer, 0, pcm, processed, read);
						processed += read;
						if(onPcmLoad != null) {
							onPcmLoad.accept(processed);
						}
					}
					if(onPcmLoad != null) {
						onPcmLoad.accept(pcm.length);
					}
				} catch(InterruptedIOException ignored) {
				} catch(IOException e) {
					LOGGER.error("Unexpected IOException", e);
				} catch(Exception e) {
					LOGGER.error("Unexpected Exception", e);
				}
			});
		} else {
			LOADING_EXECUTOR.interrupt();
		}
	}

	private static void maybeStart() {
		if(audioThread != null) {
			return;
		}
		audioThread = new Thread("Audio") {
			@Override
			public void run() {

				SourceDataLine line = null;
				PASimple paStream = null;

				try {
					paStream = new PASimple(null, "PMP", false, null, "Playback",
							new SampleSpec(SampleFormat.S16LE, (int) PLAYBACK_AUDIO_FORMAT.getSampleRate(),
									(short) PLAYBACK_AUDIO_FORMAT.getChannels()), null);
					LOGGER.info("Using PulseAudio");
				} catch(OperationNotSupportedException ex) {
					LOGGER.info("Using SourceDataLine");

					Line.Info info = new DataLine.Info(SourceDataLine.class, PLAYBACK_AUDIO_FORMAT,
							PLAYBACK_BUFFER_SIZE);

					try {
						line = (SourceDataLine) AudioSystem.getLine(info);
						line.open(PLAYBACK_AUDIO_FORMAT, PLAYBACK_BUFFER_SIZE);
						line.start();
					} catch(LineUnavailableException e) {
						LOGGER.error("Line unavailable (cannot play audio)", e);
						return;
					}
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
								if(paStream != null) {
									paStream.write(pcm, framePosition, pcm.length - framePosition);
								} else {
									assert line != null;
									line.write(pcm, framePosition, pcm.length - framePosition);
								}

								pause();
								break;
							}

							if(paStream != null) {
								paStream.write(pcm, framePosition, PLAYBACK_BUFFER_SIZE);
							} else {
								assert line != null;
								line.write(pcm, framePosition, PLAYBACK_BUFFER_SIZE);
							}

							framePosition += PLAYBACK_BUFFER_SIZE;
						}
						shouldSeek = false;
					}
				} catch(InterruptedException e) {
					LOGGER.warn("Stopping audio thread due to interrupt");
				} catch(PulseAudioException e) {
					LOGGER.error("Unexpected PulseAudio error", e);
				} finally {
					if(paStream != null) {
						paStream.close();
					}
				}
			}
		};
		audioThread.start();
	}

	public record TrackChangeEvent(Track track, CompletableFuture<byte[]> picture) {}

	public record CurrentTrackLoadEvent(Integer loaded, Integer total) {}
}
