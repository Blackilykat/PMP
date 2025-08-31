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

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.blackilykat.pmp.Filter;
import dev.blackilykat.pmp.Order;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;

import java.time.Instant;
import java.util.List;

/**
 * Updates an existing playback session which is known by both the server and the client.
 */
public class PlaybackSessionUpdateMessage extends Message {
	public static final String MESSAGE_TYPE = "PlaybackSessionUpdate";

	public Integer sessionId;
	public String track;
	public ShuffleOption shuffle;
	public RepeatOption repeat;
	public Boolean playing;
	public Integer position;
	public Integer owner;
	public List<Filter> filters;
	public Integer sortingHeader;
	public Order sortingOrder;
	public Instant time;

	public PlaybackSessionUpdateMessage(int sessionId) {
		this(sessionId, null, null, null, null, null, null, null, null, null, null);
	}

	/**
	 * @param sessionId The session to update
	 * @param track The filename of new track that's playing, null if unchanged
	 * @param shuffle The new shuffle option, null if unchanged
	 * @param repeat The new repeat option, null if unchanged
	 * @param playing Whether it's currently playing or not, null if unchanged
	 * @param position The new position, null if unchanged (This should not be sent during normal progression of a
	 * track, but only at jumps)
	 * @param owner The device id of the owner, -1 if no owner, null if unchanged
	 * @param filters The new filters, null if unchanged. Should be re-sent every time anything is changed, including
	 * changing an option.
	 * @param sortingHeader The id of the new header on which to sort tracks, null if unchanged.
	 * @param sortingOrder The order in which to sort tracks, null if unchanged.
	 * @param time When the update happened. Used to take latency into account when updating the position. Optional if
	 * position is null.
	 */
	@JsonCreator
	public PlaybackSessionUpdateMessage(Integer sessionId, String track, ShuffleOption shuffle, RepeatOption repeat,
			Boolean playing, Integer position, Integer owner, List<Filter> filters, Integer sortingHeader,
			Order sortingOrder, Instant time) {
		this.sessionId = sessionId;
		this.track = track;
		this.shuffle = shuffle;
		this.repeat = repeat;
		this.playing = playing;
		this.position = position;
		this.owner = owner;
		this.filters = filters;
		this.sortingHeader = sortingHeader;
		this.sortingOrder = sortingOrder;
		this.time = time;
	}
}
