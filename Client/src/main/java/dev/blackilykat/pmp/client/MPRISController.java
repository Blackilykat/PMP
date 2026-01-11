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

import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.util.Pair;
import dev.blackilykat.pmp.util.Shutdown;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.DBusException;
import org.mpris.MPRIS;
import org.mpris.MPRISBuilder;
import org.mpris.Metadata;
import org.mpris.mpris.PlaybackStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MPRISController {
	private static final Logger LOGGER = LogManager.getLogger(MPRISController.class);

	public static MPRIS mpris = null;
	private static boolean initialized = false;
	private static Path albumArtTempFile = null;

	public static void init() {
		if(initialized) {
			throw new IllegalStateException("Already initialized");
		}
		initialized = true;

		try {
			mpris = new MPRISBuilder()

					.setCanQuit(true).setOnQuit(() -> Shutdown.shutdown(true))

					.setCanRaise(false)

					.setIdentity("PMP")

					.setCanControl(true)

					.setCanSeek(true).setOnSeek(us -> Player.seek(us / 1000))

					.setOnSetPosition(us -> Player.seek(us.getPosition() / 1000))

					.setCanPlay(true).setOnPlay(() -> Player.play())

					.setCanPause(true).setOnPause(() -> Player.pause())

					.setOnPlayPause(() -> Player.playPause())

					.setCanGoPrevious(true).setOnPrevious(() -> Player.previous())

					.setCanGoNext(true).setOnNext(() -> Player.next())

					.setOnShuffle(s -> Player.setShuffle(s ? ShuffleOption.ON : ShuffleOption.OFF))

					.build("PMP");


			Player.EVENT_TRACK_CHANGE.register(event -> {
				Track track = event.track();
				if(track == null) {
					return;
				}

				Map<String, List<String>> metadata = new HashMap<>();
				for(Pair<String, String> metadatum : track.metadata) {
					String key = metadatum.key.toLowerCase();
					if(!metadata.containsKey(key)) {
						metadata.put(key, new LinkedList<>());
					}
					metadata.get(key).add(metadatum.value);
				}

				Metadata.Builder metadataBuilder = new Metadata.Builder().setXesamMetadata(metadata)
						.setURL(track.getFile().toURI())
						.setTrackID(new DBusPath("/PMP/" + track.getFile()
								.getName()
								.replace(".flac", "")
								.replaceAll("[^A-Za-z0-9_]", "")));

				event.picture().thenAccept(bytes -> {
					if(albumArtTempFile != null) {
						var _ = albumArtTempFile.toFile().delete();
						albumArtTempFile = null;
					}
					if(bytes != null) {
						try {
							albumArtTempFile = Files.createTempFile("pmp_album_art_", "");
							Files.write(albumArtTempFile, bytes);
							metadataBuilder.setArtURL(albumArtTempFile.toUri());
						} catch(IOException e) {
							LOGGER.error("Failed to store album art for MPRIS", e);
						}
					}

					try {
						mpris.setMetadata(metadataBuilder.build());
					} catch(DBusException e) {
						LOGGER.error("Failed to emit track change event to MPRIS", e);
					}
				});
			});

			Player.EVENT_PLAY_PAUSE.register(paused -> {
				try {
					mpris.setPlaybackStatus(paused ? PlaybackStatus.PAUSED : PlaybackStatus.PLAYING);
				} catch(DBusException e) {
					LOGGER.error("Failed to emit play/pause event to MPRIS", e);
				}
			});

			Player.EVENT_SEEK.register(ms -> {
				mpris.setPosition((int) (ms * 1000));
			});

			// setPosition does not send an update, it is not a problem to call it often
			Player.EVENT_PROGRESS.register(ms -> {
				mpris.setPosition((int) (ms * 1000));
			});

			Player.EVENT_SHUFFLE_CHANGED.register(shuffle -> {
				try {
					mpris.setShuffle(shuffle == ShuffleOption.ON);
				} catch(DBusException e) {
					LOGGER.error("Failed to emit shuffle event to MPRIS", e);
				}
			});

			Shutdown.EVENT_SHUTDOWN.register(_ -> {
				if(albumArtTempFile != null) {
					var _ = albumArtTempFile.toFile().delete();
				}
			});
		} catch(DBusException e) {
			// info because it may be expected for MPRIS not to work
			LOGGER.info("Failed to start mpris", e);
		}
	}
}
