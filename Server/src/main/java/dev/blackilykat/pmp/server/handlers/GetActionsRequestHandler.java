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

import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.MessageHandler;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.messages.ErrorMessage;
import dev.blackilykat.pmp.messages.GetActionsRequest;
import dev.blackilykat.pmp.messages.GetActionsResponse;
import dev.blackilykat.pmp.server.ServerStorage;

import java.util.List;

public class GetActionsRequestHandler extends MessageHandler<GetActionsRequest> {
	public GetActionsRequestHandler() {
		super(GetActionsRequest.class);
	}

	@Override
	public void run(PMPConnection connection, GetActionsRequest message) {
		ServerStorage ss = ServerStorage.getInstance();
		List<Action> allActions = ss.getActions();
		try {
			connection.send(
					new GetActionsResponse(allActions.subList(message.from, allActions.size()), message.requestId));
		} catch(IndexOutOfBoundsException e) {
			connection.send(new ErrorMessage("Invalid 'from' value: " + e.getMessage()));
		}
	}
}
