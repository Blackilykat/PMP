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
 * Request to log in using a password. This can contain either a password or a token: password login must be used only
 * as a fallback in case the token is incorrect.
 * <p>Direction: C2S
 */
public class LoginAsExistingDeviceRequest extends Request {
	public static final String MESSAGE_TYPE = "LoginAsExistingDevice";

	public String password;
	public String token;
	public int deviceId;

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

	public static LoginAsExistingDeviceRequest newWithPassword(String password, int deviceId) {
		LoginAsExistingDeviceRequest req = new LoginAsExistingDeviceRequest(deviceId);
		req.password = password;
		return req;
	}

	public static LoginAsExistingDeviceRequest newWithToken(String token, int deviceId) {
		LoginAsExistingDeviceRequest req = new LoginAsExistingDeviceRequest(deviceId);
		req.token = token;
		return req;
	}
}

