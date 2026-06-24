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

/// A login request was rejected.
///
/// Direction: S2C
///
/// @see LoginAsExistingDeviceRequest
/// @see LoginAsNewDeviceRequest
public class LoginFailResponse extends Response {
	public static final String MESSAGE_TYPE = "LoginFail";

	/// Machine readable reason for why the login has failed.
	public Reason reason;

	public LoginFailResponse(Integer requestId, Reason reason) {
		super(requestId);
		this.reason = reason;
	}

	/// Machine readable reason for why the login has failed.
	public enum Reason {
		/// Incorrect password or token, depending on which one was used.
		INCORRECT_CREDENTIALS,

		/// The client tried to log in with a device ID which does not exist.
		///
		/// This should only happen if an attacker is attempting to gain unauthorized access, but
		/// may also happen in case the server gets brutally interrupted between sending login confirmation
		/// and saving storage.
		NO_SUCH_DEVICE,

		/// The client sent an invalid request. This should never happen with an official client.
		BAD_REQUEST,

		/// The device is already connected.
		///
		/// This may be due to:
		/// - the user mistakenly starting a client multiple times;
		/// - a connection silently dropping with the client reconnecting faster than the server can timeout the connection;
		/// - an attacker with correct credentials attempting to log in as an existing device.
		DEVICE_ALREADY_CONNECTED
	}
}
