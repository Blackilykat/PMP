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

/// Used to indicate that the playback owner has updated information
///
/// Direction: C2S, C2S2C
public class PlaybackUpdateMessage extends Message {
	public static final String MESSAGE_TYPE = "PlaybackUpdate";

	/// Whether playback is playing or paused or null if unchanged.
	public Boolean playing = null;

	/// If playing the relative epoch of when the song started, else the position in milliseconds. Null if unchanged.
	public Long positionOrEpoch = null;

	/// The selected shuffle option or null if unchanged.
	public ShuffleOption shuffle = null;

	/// The selected repeat option or null if unchanged.
	public RepeatOption repeat = null;

	/// The filename of the currently playing track or null if unchanged.
	public String track = null;

	/// The list of selected positive options or null if unchanged.
	///
	/// The list contains a pair of filter ids and option names.
	public List<Pair<Integer, String>> positiveOptions = null;

	/// The list of selected negative options or null if unchanged.
	///
	/// The list contains a pair of filter ids and option names.
	public List<Pair<Integer, String>> negativeOptions = null;

	public PlaybackUpdateMessage() {
	}
}
