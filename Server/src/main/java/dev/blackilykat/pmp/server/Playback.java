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

package dev.blackilykat.pmp.server;

import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.messages.LoginSuccessResponse;
import dev.blackilykat.pmp.messages.PlaybackControlMessage;
import dev.blackilykat.pmp.messages.PlaybackOwnershipMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;

import java.time.Instant;

/// Management of the state of playback.
public class Playback {
	/// The device which currently owns playback.
	///
	/// This is the device which is meant to be playing audio.
	///
	/// The playback owner has the final say on any playback update. Devices which do not own playback
	/// are allowed to send [PlaybackControlMessage]s but cannot directly call updates. Only the playback
	/// owner can send [PlaybackUpdateMessage]s which get broadcasted to all connected devices.
	public static Device owner = null;

	/// True if playing, false if paused. Does not get stored.
	public static boolean playing = false;

	/// If [#playing], the epoch of when playback started.
	///
	/// Else, the position in milliseconds in the track.
	public static long positionOrEpoch;

	/// Initializes saving and storing state to storage and register events related to the playback [#owner].
	public static void init() {
		positionOrEpoch = ServerStorage.MAIN.position.get();

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

		ServerStorage.MAIN.eventMaybeSaving.register(storage -> {
			if(playing) {
				storage.markDirty();
				return;
			}
			if(positionOrEpoch != ((ServerStorage.Main) storage).position.get()) {
				storage.markDirty();
			}
		});

		ServerStorage.MAIN.eventSaving.register(s -> {
			ServerStorage.Main main = (ServerStorage.Main) s;
			if(playing) {
				main.position.set(Instant.now().toEpochMilli() - positionOrEpoch);
			} else {
				main.position.set(positionOrEpoch);
			}
		});
	}

	/// Fill fields of a [LoginSuccessResponse] with information related to playback.
	public static void fillLoginSuccessResponse(LoginSuccessResponse response) {
		response.playbackOwner = owner == null ? null : owner.id;
		response.playing = playing;
		response.track = ServerStorage.MAIN.track.get();
		response.positionOrEpoch = positionOrEpoch;
		response.positiveOptions = ServerStorage.MAIN.positiveFilterOptions.get();
		response.negativeOptions = ServerStorage.MAIN.positiveFilterOptions.get();
		response.repeat = ServerStorage.MAIN.repeat.get();
		response.shuffle = ServerStorage.MAIN.shuffle.get();
	}
}
