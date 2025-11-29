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
import dev.blackilykat.pmp.messages.PlaybackOwnershipMessage;
import dev.blackilykat.pmp.server.ClientConnection;
import dev.blackilykat.pmp.server.Device;
import dev.blackilykat.pmp.server.Playback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlaybackOwnershipMessageHandler extends MessageHandler<PlaybackOwnershipMessage> {
	private static final Logger LOGGER = LogManager.getLogger(PlaybackOwnershipMessageHandler.class);

	public PlaybackOwnershipMessageHandler() {
		super(PlaybackOwnershipMessage.class);
	}

	@Override
	public void run(PMPConnection pmpConnection, PlaybackOwnershipMessage message) {
		ClientConnection connection = (ClientConnection) pmpConnection;
		if(message.deviceId != null) {
			connection.send(new ErrorMessage("C2S SessionOwnershipMessage should not have a device ID"));
			return;
		}

		Playback.owner = connection.device;

		PlaybackOwnershipMessage outMessage = new PlaybackOwnershipMessage(connection.device.id);
		Device.broadcastExcept(outMessage, connection.device);
	}
}