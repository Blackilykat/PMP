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

/// Request to log in as an existing device. This can contain either a password or a
/// token: password login must be used only as a fallback in case the token is incorrect.
///
/// Direction: C2S
public class LoginAsExistingDeviceRequest extends Request {
	public static final String MESSAGE_TYPE = "LoginAsExistingDevice";

	/// The user-entered password, if a previous attempt at using the token has failed.
	///
	/// If this is set, [#token] must be null.
	public String password;

	/// The previously stored token.
	///
	/// If this is set, [#password] must be null.
	public String token;

	/// The previously stored device ID the client is attempting to log in as.
	public int deviceId;

	/// Private constructor used in [#newWithPassword] and [#newWithToken] and by the JSON deserializer.
	private LoginAsExistingDeviceRequest(int deviceId) {
		this.deviceId = deviceId;
	}

	@Override
	public Message withRedactedInfo() {
		LoginAsExistingDeviceRequest clone = (LoginAsExistingDeviceRequest) clone();
		if(clone.password != null) {
			clone.password = "REDACTED";
		}
		if(clone.token != null) {
			clone.token = "REDACTED";
		}
		return clone;
	}

	/// Create a login request using a user-entered password.
	///
	/// Use only as a fallback in case an attempt at using the token failed.
	///
	/// @param password the user entered password
	/// @param deviceId the previously stored device ID
	/// @return a new [LoginAsExistingDeviceRequest]
	public static LoginAsExistingDeviceRequest newWithPassword(String password, int deviceId) {
		LoginAsExistingDeviceRequest req = new LoginAsExistingDeviceRequest(deviceId);
		req.password = password;
		return req;
	}

	/// Create a login request using a previously stored token.
	///
	/// @param token the previously stored token
	/// @param deviceId the previously stored device ID
	/// @return a new [LoginAsExistingDeviceRequest]
	public static LoginAsExistingDeviceRequest newWithToken(String token, int deviceId) {
		LoginAsExistingDeviceRequest req = new LoginAsExistingDeviceRequest(deviceId);
		req.token = token;
		return req;
	}
}

