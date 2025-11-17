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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * After a request, the client is now logged in.
 * <p>Direction: S2C
 */
public class LoginSuccessResponse extends Response {
	public static final String MESSAGE_TYPE = "LoginSuccess";

	private static final Logger LOGGER = LogManager.getLogger(LoginSuccessResponse.class);

	public Integer deviceId = null;
	public String token;

	public LoginSuccessResponse(Integer requestId, Integer deviceId, String token) {
		super(requestId);
		this.token = token;
		this.deviceId = deviceId;
		if(token == null) {
			LOGGER.warn("Created login success response without token");
		}
		if(deviceId == null) {
			LOGGER.warn("Created login success response without deviceId");
		}
	}

	@Override
	public Message withRedactedInfo() {
		LoginSuccessResponse clone = (LoginSuccessResponse) clone();
		if(clone.token != null) {
			clone.token = "REDACTED";
		}
		return clone;
	}
}
