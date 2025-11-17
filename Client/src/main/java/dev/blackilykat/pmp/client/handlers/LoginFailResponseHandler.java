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

package dev.blackilykat.pmp.client.handlers;

import dev.blackilykat.pmp.MessageHandler;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Server;
import dev.blackilykat.pmp.messages.LoginFailResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginFailResponseHandler extends MessageHandler<LoginFailResponse> {
	private static final Logger LOGGER = LogManager.getLogger(LoginFailResponseHandler.class);

	public LoginFailResponseHandler() {
		super(LoginFailResponse.class);
	}

	@Override
	public void run(PMPConnection connection, LoginFailResponse message) {
		ClientStorage cs = ClientStorage.getInstance();
		switch(message.reason) {
			case INCORRECT_CREDENTIALS -> {
				cs.setToken(null);
				Server.EVENT_SHOULD_ASK_PASSWORD.call(null);
			}
			case NO_SUCH_DEVICE -> {
				cs.setDeviceID(null);
				cs.setToken(null);
				Server.EVENT_SHOULD_ASK_PASSWORD.call(null);
			}
			case BAD_REQUEST -> {
				LOGGER.error("Failed login with BAD_REQUEST, are client and server versions mismatched?");
			}
			case DEVICE_ALREADY_CONNECTED -> {
				// Maybe the connection dropped without TCP FIN packets? disconnect, try to let it reconnect in a
				// while and see if the server noticed the lack of keepalives
				connection.disconnect("Device is already connected");
			}
		}
	}
}
