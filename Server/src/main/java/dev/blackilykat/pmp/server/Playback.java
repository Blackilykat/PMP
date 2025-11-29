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

package dev.blackilykat.pmp.server;

import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.messages.LoginSuccessResponse;
import dev.blackilykat.pmp.messages.PlaybackOwnershipMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

public class Playback {
	private static final Logger LOGGER = LogManager.getLogger(Playback.class);

	public static Device owner = null;
	public static boolean playing = false;
	public static long positionOrEpoch;


	public static void init() {
		positionOrEpoch = ServerStorage.getInstance().getPosition();

		PMPConnection.EVENT_DISCONNECTED.register(pmpConn -> {
			if(!(pmpConn instanceof ClientConnection connection)) {
				return;
			}
			if(connection.device != owner) {
				return;
			}
			owner = null;
			if(playing) {
				playing = false;
				positionOrEpoch = Instant.now().toEpochMilli() - positionOrEpoch;
			}
			Device.broadcast(new PlaybackOwnershipMessage());
			PlaybackUpdateMessage update = new PlaybackUpdateMessage();
			update.playing = false;
			update.positionOrEpoch = positionOrEpoch;
			Device.broadcast(update);
		});

		ServerStorage.EVENT_MAYBE_SAVING.register(event -> {
			if(playing) {
				event.markDirty();
				return;
			}
			if(positionOrEpoch != event.serverStorage.getPosition()) {
				event.markDirty();
			}
		});

		ServerStorage.EVENT_SAVING.register(storage -> {
			if(playing) {
				storage.setPosition(Instant.now().toEpochMilli() - positionOrEpoch);
			} else {
				storage.setPosition(positionOrEpoch);
			}
		});
	}

	public static void fillLoginSuccessResponse(LoginSuccessResponse response) {
		ServerStorage ss = ServerStorage.getInstance();
		response.playbackOwner = owner == null ? null : owner.id;
		response.playing = playing;
		response.track = ss.getTrack();
		response.positionOrEpoch = positionOrEpoch;
		response.positiveOptions = ss.getPositiveFilterOptions();
		response.negativeOptions = ss.getNegativeFilterOptions();
		response.repeat = ss.getRepeat();
		response.shuffle = ss.getShuffle();
	}
}