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

import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.util.Pair;

import java.util.List;

/// Used to indicate that a device is trying to control the playback owner
///
/// Direction: C2S2C
public class PlaybackControlMessage extends Message {
	public static final String MESSAGE_TYPE = "PlaybackControl";

	/// true to start playing, false to pause, or null
	public Boolean playing = null;

	/// Position in ms to seek to or null
	public Long position = null;

	/// New shuffle state to set or null
	public ShuffleOption shuffle = null;

	/// New repeat state to set or null
	public RepeatOption repeat = null;

	/// Filename of track to start playing or null
	public String track = null;

	/// New positive filter options or null
	///
	/// The list contains a pair of filter ids and option names.
	public List<Pair<Integer, String>> positiveOptions = null;

	/// New negative filter options or null
	///
	/// The list contains a pair of filter ids and option names.
	public List<Pair<Integer, String>> negativeOptions = null;

	public PlaybackControlMessage() {
	}
}
