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

package dev.blackilykat.pmp.server.handlers;

import dev.blackilykat.pmp.MessageHandler;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.messages.ErrorMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;
import dev.blackilykat.pmp.server.ClientConnection;
import dev.blackilykat.pmp.server.Device;
import dev.blackilykat.pmp.server.Playback;
import dev.blackilykat.pmp.server.ServerStorage;

public class PlaybackUpdateMessageHandler extends MessageHandler<PlaybackUpdateMessage> {
	public PlaybackUpdateMessageHandler() {
		super(PlaybackUpdateMessage.class);
	}

	@Override
	public void run(PMPConnection pmpConn, PlaybackUpdateMessage message) {
		if(!(pmpConn instanceof ClientConnection connection)) {
			return;
		}
		if(connection.device != Playback.owner) {
			connection.send(new ErrorMessage("Only the playback owner can send playback updates."));
			return;
		}

		if(message.playing != null) {
			Playback.playing = message.playing;
		}
		if(message.shuffle != null) {
			ServerStorage.MAIN.shuffle.set(message.shuffle);
		}
		if(message.repeat != null) {
			ServerStorage.MAIN.repeat.set(message.repeat);
		}
		if(message.track != null) {
			ServerStorage.MAIN.track.set(message.track);
		}
		if(message.positionOrEpoch != null) {
			Playback.positionOrEpoch = message.positionOrEpoch;
		}
		if(message.negativeOptions != null) {
			ServerStorage.MAIN.negativeFilterOptions.set(message.negativeOptions);
		}
		if(message.positiveOptions != null) {
			ServerStorage.MAIN.positiveFilterOptions.set(message.positiveOptions);
		}

		Device.broadcastExcept(message, connection.device);
	}
}
