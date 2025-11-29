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

import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.util.Pair;

import java.util.List;

/**
 * Used to indicate that a device is trying to control the playback owner
 * <p>Direction: Bidirectional (C2S2C)
 */
public class PlaybackControlMessage extends Message {
	public static final String MESSAGE_TYPE = "PlaybackControl";

	public Boolean playing = null;
	public Long position = null;
	public ShuffleOption shuffle = null;
	public RepeatOption repeat = null;
	public String track = null;
	public List<Pair<Integer, String>> positiveOptions = null;
	public List<Pair<Integer, String>> negativeOptions = null;

	public PlaybackControlMessage() {
	}
}
