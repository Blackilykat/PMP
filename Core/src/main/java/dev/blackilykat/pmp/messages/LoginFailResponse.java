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
 * A login request was rejected.
 * <p>Direction: S2C
 */
public class LoginFailResponse extends Response {
	public static final String MESSAGE_TYPE = "LoginFail";
	public Reason reason;

	public LoginFailResponse(Integer requestId, Reason reason) {
		super(requestId);
		this.reason = reason;
	}

	public enum Reason {
		INCORRECT_CREDENTIALS, NO_SUCH_DEVICE, BAD_REQUEST, DEVICE_ALREADY_CONNECTED
	}
}
