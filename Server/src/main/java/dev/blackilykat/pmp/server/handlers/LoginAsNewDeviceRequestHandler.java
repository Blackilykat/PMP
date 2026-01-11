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
import dev.blackilykat.pmp.messages.LoginAsNewDeviceRequest;
import dev.blackilykat.pmp.messages.LoginFailResponse;
import dev.blackilykat.pmp.messages.LoginSuccessResponse;
import dev.blackilykat.pmp.server.ClientConnection;
import dev.blackilykat.pmp.server.Device;
import dev.blackilykat.pmp.server.Playback;
import dev.blackilykat.pmp.server.ServerStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginAsNewDeviceRequestHandler extends MessageHandler<LoginAsNewDeviceRequest> {
	private static final Logger LOGGER = LogManager.getLogger(LoginAsNewDeviceRequestHandler.class);

	public LoginAsNewDeviceRequestHandler() {
		super(LoginAsNewDeviceRequest.class);
	}

	@Override
	public void run(PMPConnection pmpConnection, LoginAsNewDeviceRequest message) {
		ClientConnection connection = (ClientConnection) pmpConnection;

		if(message.password == null) {
			connection.send(new LoginFailResponse(message.requestId, LoginFailResponse.Reason.BAD_REQUEST));
			return;
		}

		if(message.hostname == null) {
			LOGGER.warn("Received LoginAsNewDeviceRequest without hostname");
			connection.send(new LoginFailResponse(message.requestId, LoginFailResponse.Reason.BAD_REQUEST));
			return;
		}

		if(!message.password.equals(ServerStorage.SENSITIVE.password.get())) {
			connection.send(new LoginFailResponse(message.requestId, LoginFailResponse.Reason.INCORRECT_CREDENTIALS));

			// Make brute-forcing attacks less viable. This sleeps in the specific client's input thread, no
			// other clients are impacted.
			try {
				Thread.sleep(2000);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return;
		}

		Device device = new Device(message.hostname);
		device.rerollToken();
		device.setClientConnection(connection);

		ServerStorage.SENSITIVE.devices.add(device);

		connection.device = device;
		LoginSuccessResponse response = new LoginSuccessResponse(message.requestId, device.id, device.getToken(),
				ServerStorage.MAIN.actions.size() - 1);
		Playback.fillLoginSuccessResponse(response);
		FilterListMessageHandler.fillLoginSuccessResponse(response);
		connection.send(response);
	}
}