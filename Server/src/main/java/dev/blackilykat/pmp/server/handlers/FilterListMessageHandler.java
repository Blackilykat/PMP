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
import dev.blackilykat.pmp.messages.FilterListMessage;
import dev.blackilykat.pmp.messages.LoginSuccessResponse;
import dev.blackilykat.pmp.server.ClientConnection;
import dev.blackilykat.pmp.server.Device;
import dev.blackilykat.pmp.server.ServerStorage;

import java.util.LinkedList;

public class FilterListMessageHandler extends MessageHandler<FilterListMessage> {
	public FilterListMessageHandler() {
		super(FilterListMessage.class);
	}

	@Override
	public void run(PMPConnection pmpConn, FilterListMessage message) {
		if(!(pmpConn instanceof ClientConnection connection)) {
			return;
		}
		Device.broadcastExcept(message, connection.device);

		ServerStorage.MAIN.filters.set(message.filters);
	}

	public static void fillLoginSuccessResponse(LoginSuccessResponse response) {
		response.filters = new LinkedList<>(ServerStorage.MAIN.filters.get());
	}
}
