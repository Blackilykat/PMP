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

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Message sent by the client to log in.
 * <p>
 * It can contain one of the following combinations:
 * <ul>
 *     <li>Password and hostname to identify as a new device</li>
 *     <li>Token and device id if it has already been assigned a device id</li>
 *     <li>Password and device id if, for whatever reason, the token was incorrect. This would theoretically never
 *     happen unless someone else logged in using it. In practice, it has happened before due to unexpected termination
 *     of either program.</li>
 * </ul>
 * Tokens are re-rolled at every login. They are sent in {@link WelcomeMessage} after each successful login.
 */
public class LoginMessage extends Message {
	public static final String MESSAGE_TYPE = "Login";

	public String password = null;
	public String hostname = null;
	public String token = null;
	public Integer deviceId = null;

	@JsonCreator
	private LoginMessage() {
	}

	public LoginMessage(String password, String hostname) {
		this.password = password;
		this.hostname = hostname;
	}

	public LoginMessage(String tokenOrPassword, int deviceId, boolean isToken) {
		if(isToken) {
			this.token = tokenOrPassword;
		} else {
			this.password = tokenOrPassword;
		}
		this.deviceId = deviceId;
	}
}
