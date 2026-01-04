/*
 * Copyright (C) 2026 Blackilykat and contributors
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
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.event.RetroactiveEventSource;
import dev.blackilykat.pmp.messages.PlaybackControlMessage;
import dev.blackilykat.pmp.messages.PlaybackOwnershipMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;
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
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Player {
	public static final RetroactiveEventSource<Boolean> EVENT_PLAY_PAUSE = new RetroactiveEventSource<>();
	public static final RetroactiveEventSource<TrackChangeEvent> EVENT_TRACK_CHANGE = new RetroactiveEventSource<>();
	public static final RetroactiveEventSource<Long> EVENT_PROGRESS = new RetroactiveEventSource<>();
	public static final RetroactiveEventSource<CurrentTrackLoadEvent> EVENT_CURRENT_TRACK_LOAD =
			new RetroactiveEventSource<>();
	public static final RetroactiveEventSource<PlaybackDebugInfoEvent> EVENT_PLAYBACK_DEBUG_INFO =
			new RetroactiveEventSource<>();
	public static final RetroactiveEventSource<RepeatOption> EVENT_REPEAT_CHANGED = new RetroactiveEventSource<>();
	public static final RetroactiveEventSource<ShuffleOption> EVENT_SHUFFLE_CHANGED = new RetroactiveEventSource<>();
	public static final RetroactiveEventSource<Track> EVENT_NEXT_TRACK_CHANGED = new RetroactiveEventSource<>();
	public static final EventSource<Long> EVENT_SEEK = new EventSource<>();
	public static final EventSource<Integer> EVENT_PLAYBACK_OWNER_CHANGED = new EventSource<>();
	public static final ScopedValue<Boolean> DONT_SEND_UPDATES = ScopedValue.newInstance();
	private static final Logger LOGGER = LogManager.getLogger(Player.class);
	private static final ScopedValue<Boolean> APPLYING_SESSION = ScopedValue.newInstance();
	private static final AudioFormat PLAYBACK_AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100,
			16,
			2, 4, 44100, false);

	/**
	 * Buffer size when playing the already loaded audio. Too high values can make pausing unresponsive. Too low values
	 * can make audio choppy in some circumstances.
	 */
	private static final int PLAYBACK_BUFFER_SIZE = 8192;
	/**
	 * Cooldown multiplier when writing to audio lines. Cooldown is calculated using buffer size and audio format. This
	 * multiplier is then applied to leave some wiggle room and allow the audio backend to build a small buffer.
	 */
	private static final double PLAYBACK_WRITE_COOLDOWN_MULTIPLIER = 0.9;
	/**
	 * Buffer size when loading track from disk. Too low values can increase CPU usage. Too high values can make it
	 * take
	 * longer to start playing.
	 */
	private static final int LOADING_BUFFER_SIZE =
			(int) PLAYBACK_AUDIO_FORMAT.getSampleRate() * PLAYBACK_AUDIO_FORMAT.getChannels();

	private static final OverridingSingleThreadExecutor DECODING_EXECUTOR = new OverridingSingleThreadExecutor();
	private static final OverridingSingleThreadExecutor LOADING_EXECUTOR = new OverridingSingleThreadExecutor();
	private static final Pair<ScheduledExecutorService, ScheduledFuture<?>> PROGRESS_EXECUTOR = new Pair<>(
			Executors.newSingleThreadScheduledExecutor(), null);
	private static final AtomicBoolean paused = new AtomicBoolean(true);
	private static final AtomicReference<Integer> playbackOwner = new AtomicReference<>(null);
	private static RepeatOption repeat = RepeatOption.OFF;
	private static ShuffleOption shuffle = ShuffleOption.OFF;
	private static Track nextTrack = null;
	private static boolean shouldSeek = false;
	private static boolean shouldCheckOwnership = false;
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
	private static PASimple paStream = null;
	private static SourceDataLine line = null;

	public static boolean shouldSendControl() {
		return !DONT_SEND_UPDATES.orElse(false) && Server.isLoggedIn() && playbackOwner.get() != null
				&& !playbackOwner.get().equals(Server.deviceId);
	}

	public static boolean shouldSendUpdate() {
		return !DONT_SEND_UPDATES.orElse(false) && Server.isLoggedIn() && Objects.equals(playbackOwner.get(),
				Server.deviceId);
	}

	public static void takeOwnershipIfNeeded() {
		if(DONT_SEND_UPDATES.orElse(false)) {
			return;
		}
		if(Server.isLoggedIn() && playbackOwner.get() == null) {
			setPlaybackOwner(Server.deviceId);
			Server.send(new PlaybackOwnershipMessage());
		}
	}

	public static void play() {
		if(shouldSendControl()) {
			PlaybackControlMessage msg = new PlaybackControlMessage();
			msg.playing = true;
			Server.send(msg);
			return;
		}
		takeOwnershipIfNeeded();

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
		if(shouldSendControl()) {
			PlaybackControlMessage msg = new PlaybackControlMessage();
			msg.playing = false;
			Server.send(msg);
			return;
		}
		takeOwnershipIfNeeded();

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
		if(shouldSendControl()) {
			PlaybackControlMessage msg = new PlaybackControlMessage();
			msg.playing = true;
			msg.position = 0L;
			msg.track = track.getFile().getName();
			Server.send(msg);
			return;
		}

		takeOwnershipIfNeeded();

		pause();
		currentTrack = track;
		LOGGER.info("Playing {}", track.getTitle());

		shouldSeek = true;
		setPosition(0);

		load(track, true, true);
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

	public static long getPlaybackEpoch() {
		return playbackEpoch.toEpochMilli();
	}

	public static void setPlaybackEpoch(long epoch) {
		playbackEpoch = Instant.ofEpochMilli(epoch);
		shouldSeek = true;
	}

	public static void seek(long ms) {
		if(shouldSendControl()) {
			PlaybackControlMessage msg = new PlaybackControlMessage();
			msg.position = ms;
			Server.send(msg);
		}

		takeOwnershipIfNeeded();

		shouldSeek = true;

		setPosition(ms);
		EVENT_PROGRESS.call(ms);
		EVENT_SEEK.call(ms);
	}

	public static void next() {
		if(nextTrack != null) {
			play(nextTrack);
		} else {
			pause();
		}
	}

	public static void previous() {
		Track prev = findPreviousTrack();
		if(prev != null) {
			play(prev);
		} else {
			pause();
		}
	}

	public static ShuffleOption getShuffle() {
		return shuffle;
	}

	public static void setShuffle(ShuffleOption shuffle) {
		LOGGER.info("Shuffle set to {}", shuffle);
		if(shouldSendControl()) {
			PlaybackControlMessage msg = new PlaybackControlMessage();
			msg.shuffle = shuffle;
			Server.send(msg);
			return;
		}

		takeOwnershipIfNeeded();

		Player.shuffle = shuffle;
		EVENT_SHUFFLE_CHANGED.call(shuffle);
	}

	public static RepeatOption getRepeat() {
		return repeat;
	}

	public static void setRepeat(RepeatOption repeat) {
		LOGGER.info("Repeat set to {}", repeat);

		if(shouldSendControl()) {
			PlaybackControlMessage msg = new PlaybackControlMessage();
			msg.repeat = repeat;
			Server.send(msg);
			return;
		}

		takeOwnershipIfNeeded();

		Player.repeat = repeat;
		EVENT_REPEAT_CHANGED.call(repeat);
	}

	public static boolean getPaused() {
		return paused.get();
	}

	public static Track getTrack() {
		return currentTrack;
	}

	public static void load(@Nonnull Track track, boolean loadAudio, boolean startPlaying) {
		currentTrack = track;

		CompletableFuture<byte[]> albumArtFuture = new CompletableFuture<>();
		CompletableFuture<byte[]> pcmDataFuture = new CompletableFuture<>();

		if(loadAudio) {
			pcmDataFuture.thenAccept(pcmData -> {
				pcm = pcmData;
			});
		} else {
			pcm = null;
		}

		AtomicBoolean firstLoad = new AtomicBoolean(true);
		Consumer<Integer> onPcmLoad = null;
		if(loadAudio) {
			onPcmLoad = bytes -> {
				if(bytes >= PLAYBACK_AUDIO_FORMAT.getSampleRate() * PLAYBACK_AUDIO_FORMAT.getChannels()
						&& firstLoad.getAndSet(false)) {

					if(paStream != null) {
						try {
							paStream.flush();
						} catch(PulseAudioException e) {
							LOGGER.error("Failed to drain paStream", e);
						}
					}
					if(startPlaying) {
						play();
					}
				}
				EVENT_CURRENT_TRACK_LOAD.call(new CurrentTrackLoadEvent(bytes, pcm.length));
			};
		}

		load(track, albumArtFuture, pcmDataFuture, onPcmLoad);

		if(!loadAudio && startPlaying) {
			play();
		}

		EVENT_TRACK_CHANGE.call(new TrackChangeEvent(track, albumArtFuture));
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
							track.getPlaybackInfo().getTotalSamples());
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
				} catch(InterruptedIOException | InterruptedException ignored) {
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

	private static boolean checkOwnershipForbidsToPlay() {
		return Server.isLoggedIn() && !Objects.equals(playbackOwner.get(), Server.deviceId);
	}

	public static void init() {
		if(audioThread != null) {
			throw new IllegalStateException("Already initialized");
		}
		LOGGER.info("Initializing player");
		audioThread = new Thread("Audio") {
			@Override
			public void run() {
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

				double msPerWrite =
						1000 / ((PLAYBACK_AUDIO_FORMAT.getFrameRate() * PLAYBACK_AUDIO_FORMAT.getChannels() * (
								PLAYBACK_AUDIO_FORMAT.getSampleSizeInBits() / 8.0)) / PLAYBACK_BUFFER_SIZE);
				LOGGER.debug("Ms per write: {}", msPerWrite);

				long writeCooldown = (long) (msPerWrite * PLAYBACK_WRITE_COOLDOWN_MULTIPLIER);

				long lastWrite;

				try {
					mainLoop:
					//noinspection InfiniteLoopStatement
					while(true) {
						synchronized(paused) {
							if(paused.get()) {
								paused.wait();
							}
						}

						synchronized(playbackOwner) {
							if(checkOwnershipForbidsToPlay()) {
								playbackOwner.wait();
								continue;
							}
						}

						if(pcm == null) {
							// I'd normally avoid this, but when it's null it should be just about to create the array
							// and replace the value, so it should realistically be just one cycle (or a few with very
							// slow storage) of this before it can get to playing audio
							//noinspection BusyWait
							Thread.sleep(100);
							continue;
						}

						framePosition = msToPlaybackBytes(getPosition());

						double totalOffset = 0;
						lastWrite = System.nanoTime();
						while(!paused.get() && !shouldSeek) {
							if(shouldCheckOwnership) {
								shouldCheckOwnership = false;
								if(checkOwnershipForbidsToPlay()) {
									continue mainLoop;
								}
							}
							long now = System.nanoTime();
							long lastWriteDiff = now - lastWrite;
							totalOffset += msPerWrite - (lastWriteDiff / 1_000_000.0);
							lastWrite = now;

							int latency = 0;
							if(paStream != null) {
								latency = msToPlaybackBytes(paStream.getLatency() / 1_000);
							}


							int expectedPos = msToPlaybackBytes(getPosition());


							EVENT_PLAYBACK_DEBUG_INFO.call(
									new PlaybackDebugInfoEvent(paStream != null, latency, framePosition, expectedPos,
											pcm.length, PLAYBACK_BUFFER_SIZE, msPerWrite, totalOffset));


							if(totalOffset < 0) {
								LOGGER.warn("Correcting frame position: was {}, is {}, {}ms offset", framePosition,
										expectedPos, -totalOffset);
								totalOffset = 0;
								framePosition = expectedPos;
							}

							if(framePosition >= pcm.length) {
								next();
								break;
							}

							if(framePosition + PLAYBACK_BUFFER_SIZE > pcm.length) {
								if(paStream != null) {
									paStream.write(pcm, framePosition, pcm.length - framePosition);
								} else {
									assert line != null;
									line.write(pcm, framePosition, pcm.length - framePosition);
								}

								next();
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

		{
			ClientStorage cs = ClientStorage.getInstance();
			ClientStorage.PlaybackInfo pi = cs.getPlaybackInfo();
			loadPlaybackInfo(pi);
		}

		ClientStorage.EVENT_MAYBE_SAVING.register(event -> {
			ClientStorage cs = event.clientStorage;
			ClientStorage.PlaybackInfo pi = cs.getPlaybackInfo();

			if(!Player.paused.get() || !Player.getTrack().getFile().getName().equals(pi.track())
					|| Player.getPosition() != pi.position() || Player.getRepeat() != pi.repeat()
					|| Player.getShuffle() != pi.shuffle()) {
				event.markDirty();
			}

			// filter options are unlikely to change alone and not computationally cheap, they're probably not worth
			// checking
		});

		ClientStorage.EVENT_SAVING.register(storage -> {
			storage.setPlaybackInfo(new ClientStorage.PlaybackInfo(
					Player.getTrack() == null ? null : Player.getTrack().getFile().getName(), Player.getPosition(),
					Library.exportPositiveOptions(), Library.exportNegativeOptions(), Player.getRepeat(),
					Player.getShuffle()));
		});

		EVENT_TRACK_CHANGE.register(_ -> {
			selectNextTrack();
		});

		Library.EVENT_SELECTED_TRACKS_UPDATED.register(_ -> {
			selectNextTrack();
		});

		EVENT_PLAY_PAUSE.register(paused -> {
			if(!shouldSendUpdate()) {
				return;
			}
			PlaybackUpdateMessage msg = new PlaybackUpdateMessage();
			if(paused) {
				msg.playing = false;
				msg.positionOrEpoch = getPosition();
			} else {
				msg.playing = true;
				msg.positionOrEpoch = getPlaybackEpoch();
			}
			Server.send(msg);
		});

		EVENT_TRACK_CHANGE.register(event -> {
			if(!shouldSendUpdate()) {
				return;
			}
			if(event.track == null) {
				return;
			}
			PlaybackUpdateMessage msg = new PlaybackUpdateMessage();
			msg.track = event.track.getFile().getName();
			Server.send(msg);
		});

		EVENT_SEEK.register(pos -> {
			if(!shouldSendUpdate()) {
				return;
			}
			PlaybackUpdateMessage msg = new PlaybackUpdateMessage();
			if(getPaused()) {
				msg.positionOrEpoch = pos;
			} else {
				msg.positionOrEpoch = getPlaybackEpoch();
			}
			Server.send(msg);
		});

		EVENT_SHUFFLE_CHANGED.register(shuffle -> {
			if(!shouldSendUpdate()) {
				return;
			}
			PlaybackUpdateMessage msg = new PlaybackUpdateMessage();
			msg.shuffle = shuffle;
			Server.send(msg);
		});

		EVENT_REPEAT_CHANGED.register(repeat -> {
			if(!shouldSendUpdate()) {
				return;
			}
			PlaybackUpdateMessage msg = new PlaybackUpdateMessage();
			msg.repeat = repeat;
			Server.send(msg);
		});

		Server.EVENT_DISCONNECTED.register(_ -> {
			if(playbackOwner.get() != null && !playbackOwner.get().equals(Server.deviceId)) {
				pause();
				setPlaybackOwner(null);
			}
		});
	}

	private static int msToPlaybackBytes(long ms) {
		int val = ((int) ((PLAYBACK_AUDIO_FORMAT.getFrameRate() * PLAYBACK_AUDIO_FORMAT.getSampleSizeInBits()
				* PLAYBACK_AUDIO_FORMAT.getChannels() * ms) / 8000));
		val -= val % PLAYBACK_AUDIO_FORMAT.getFrameSize();
		return val;
	}

	public static void loadPlaybackInfo(ClientStorage.PlaybackInfo pi) {
		if(pi == null) {
			return;
		}

		Track track = Library.getTrackByFilename(pi.track());

		currentTrack = track;
		load(track, false, false);

		setPosition(pi.position());

		if(pi.repeat() != null) {
			setRepeat(pi.repeat());
		}
		if(pi.shuffle() != null) {
			setShuffle(pi.shuffle());
		}

		Library.importFilterOptions(pi.positiveFilterOptions(), pi.negativeFilterOptions());

		EVENT_PLAY_PAUSE.call(paused.get());
		EVENT_PROGRESS.call(getPosition());
	}

	public static void selectNextTrack() {
		nextTrack = findNextTrack();

		EVENT_NEXT_TRACK_CHANGED.call(nextTrack);
	}

	private static Track findNextTrack() {
		if(repeat == RepeatOption.TRACK) {
			return currentTrack;
		}

		List<Track> options = Library.getSelectedTracks();
		if(options.isEmpty()) {
			return null;
		}
		int current = options.indexOf(currentTrack);

		if(shuffle == ShuffleOption.ON) {
			if(options.size() == 1) {
				return options.getFirst();
			}

			// select any track except the one just played
			Random random = new Random();
			int selection = random.nextInt(options.size() - 1);
			if(current != -1 && selection >= current) {
				selection++;
			}

			return options.get(selection);
		} else if(current == -1) {
			return options.getFirst();
		} else if(current == options.size() - 1) {
			if(repeat == RepeatOption.ALL) {
				return options.getFirst();
			} else {
				return null;
			}
		} else {
			return options.get(current + 1);
		}
	}

	private static Track findPreviousTrack() {
		if(repeat == RepeatOption.TRACK || shuffle == ShuffleOption.ON) {
			return findNextTrack();
		}

		List<Track> options = Library.getSelectedTracks();
		if(options.isEmpty()) {
			return null;
		}
		int current = options.indexOf(currentTrack);

		if(current == -1) {
			return options.getLast();
		} else if(current == 0) {
			if(repeat == RepeatOption.ALL) {
				return options.getLast();
			} else {
				return null;
			}
		} else {
			return options.get(current - 1);
		}
	}

	public static Integer getPlaybackOwner() {
		return playbackOwner.get();
	}

	public static void setPlaybackOwner(Integer playbackOwner) {
		if(pcm == null && (!Server.isLoggedIn() || Objects.equals(playbackOwner, Server.deviceId))
				&& currentTrack != null) {
			// make sure audio is loaded if we own now
			load(currentTrack, true, false);
		}
		Player.playbackOwner.set(playbackOwner);
		synchronized(Player.playbackOwner) {
			Player.playbackOwner.notifyAll();
		}

		shouldCheckOwnership = true;
		EVENT_PLAYBACK_OWNER_CHANGED.call(playbackOwner);
	}

	public record TrackChangeEvent(Track track, CompletableFuture<byte[]> picture) {}

	public record CurrentTrackLoadEvent(Integer loaded, Integer total) {}

	public record PlaybackDebugInfoEvent(boolean pulse, int latency, int framePosition, int expectedPos,
			int trackLength, int bufferSize, double expectedWriteTime, double totalOffset) {}
}
