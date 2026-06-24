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

import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/// After a request, the client is now logged in.
///
/// Direction: S2C
public class LoginSuccessResponse extends Response {
	public static final String MESSAGE_TYPE = "LoginSuccess";

	private static final Logger LOGGER = LogManager.getLogger(LoginSuccessResponse.class);

	/// The device id, either newly assigned or the same in the request.
	public Integer deviceId;

	/// The new token just assigned by the server. Must be stored for the next login.
	public String token;

	// non-login info the client needs when connecting

	/// The device ID of the current playback owner. May be null.
	public Integer playbackOwner;

	/// Whether playback is playing or paused.
	public boolean playing;

	/// If playing the relative epoch of when the song started, else the position in milliseconds.
	public Long positionOrEpoch;

	/// The selected shuffle option.
	public ShuffleOption shuffle;

	/// The selected repeat option.
	public RepeatOption repeat;

	/// The filename of the currently playing track.
	public String track;

	/// The list of selected positive options.
	///
	/// The list contains a pair of filter ids and option names.
	public List<Pair<Integer, String>> positiveOptions;

	/// The list of selected negative options.
	///
	/// The list contains a pair of filter ids and option names.
	public List<Pair<Integer, String>> negativeOptions;

	/// The list of all filters.
	public List<FilterInfo> filters;

	/// The server's last action ID, used by the client to decide whether to request new actions.
	///
	/// @see GetActionsRequest
	public int lastActionId = -1;

	public LoginSuccessResponse(Integer requestId, Integer deviceId, String token, int lastActionId) {
		super(requestId);
		this.token = token;
		this.deviceId = deviceId;
		this.lastActionId = lastActionId;
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
