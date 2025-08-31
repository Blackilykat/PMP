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

package dev.blackilykat.pmp.messages;

/**
 * The first message the server sends once a client is authenticated. Used to confirm the login and send some initial
 * necessary pieces of information.
 */
public class WelcomeMessage extends Message {
	public static final String MESSAGE_TYPE = "Welcome";

	public Integer clientId;
	public Integer latestActionId;
	public String token;
	public Integer deviceId;

	public WelcomeMessage(Integer clientId, Integer latestActionId, String token, Integer deviceId) {
		this.clientId = clientId;
		this.latestActionId = latestActionId;
		this.token = token;
		this.deviceId = deviceId;
	}
}
