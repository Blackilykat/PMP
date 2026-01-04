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

package dev.blackilykat.pmp.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Response to {@link ActionMessage} which tells the client how to move ahead with library sync.
 * <p>Direction: S2C
 */
public class ActionResponse extends Response {
	public static final String MESSAGE_TYPE = "ActionResponse";

	/**
	 * The action ID assigned by the server. This is only present when {@link #type} == {@link Type#COMPLETED}.
	 */
	public Integer actionId;
	public Type type;

	public ActionResponse(Integer requestId, Type type, Integer actionId) {
		super(requestId);
		this.type = type;
		this.actionId = actionId;
	}

	@Override
	@JsonIgnore
	public boolean isLastResponse() {
		return switch(type) {
			case QUEUED, APPROVED -> false;
			default -> true;
		};
	}

	public enum Type {
		/**
		 * This action has been completed. It may be an action where data does not need to be sent (i.e. REMOVE) or one
		 * which previously received an APPROVED response and is now done uploading data.
		 */
		COMPLETED,
		/**
		 * This action requires data to be uploaded and the client has 30 seconds to start the HTTP request to upload
		 * data.
		 */
		APPROVED,
		/**
		 * This action is not valid (i.e. REMOVE on a non-existent track).
		 */
		INVALID,
		/**
		 * Another client is currently performing an action and this one has been queued. The server will send another
		 * response when it's this client's turn.
		 */
		QUEUED
	}
}
