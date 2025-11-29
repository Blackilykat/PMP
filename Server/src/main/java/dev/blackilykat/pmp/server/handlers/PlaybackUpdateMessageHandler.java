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
		ServerStorage ss = ServerStorage.getInstance();

		if(message.playing != null) {
			Playback.playing = message.playing;
		}
		if(message.shuffle != null) {
			ss.setShuffle(message.shuffle);
		}
		if(message.repeat != null) {
			ss.setRepeat(message.repeat);
		}
		if(message.track != null) {
			ss.setTrack(message.track);
		}
		if(message.positionOrEpoch != null) {
			Playback.positionOrEpoch = message.positionOrEpoch;
		}
		if(message.negativeOptions != null) {
			ss.setNegativeFilterOptions(message.negativeOptions);
		}
		if(message.positiveOptions != null) {
			ss.setPositiveFilterOptions(message.positiveOptions);
		}

		Device.broadcastExcept(message, connection.device);
	}
}
