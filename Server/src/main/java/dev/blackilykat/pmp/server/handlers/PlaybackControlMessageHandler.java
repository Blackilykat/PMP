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
import dev.blackilykat.pmp.messages.PlaybackControlMessage;
import dev.blackilykat.pmp.server.ClientConnection;
import dev.blackilykat.pmp.server.Playback;

public class PlaybackControlMessageHandler extends MessageHandler<PlaybackControlMessage> {
	public PlaybackControlMessageHandler() {
		super(PlaybackControlMessage.class);
	}

	@Override
	public void run(PMPConnection connection, PlaybackControlMessage message) {
		if(Playback.owner == null) {
			connection.send(new ErrorMessage("There is no playback owner to forward the PlaybackControlMessage to"));
			return;
		}
		ClientConnection owner = Playback.owner.getClientConnection();
		assert owner != null;
		owner.send(message);
	}
}
