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

/// Used to claim playback ownership and to indicate ownership has changed.
///
/// C2S: Claim ownership of playback, device == null
///
/// (C2)S2C: Playback owner has changed, device == null **only if there is no longer an owner**
///
/// Direction: C2S, S2C, C2S2C
public class PlaybackOwnershipMessage extends Message {
	public static final String MESSAGE_TYPE = "PlaybackOwnership";

	/// The id of the new playback owner or null if none.
	///
	/// Only present when (C2)S2C.
	public Integer deviceId;

	/// Create a message with no device ID
	public PlaybackOwnershipMessage() {
		this.deviceId = null;
	}

	/// Create a message with the specified device ID
	public PlaybackOwnershipMessage(int deviceId) {
		this.deviceId = deviceId;
	}
}
