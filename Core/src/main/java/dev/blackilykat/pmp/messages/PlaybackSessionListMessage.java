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

import dev.blackilykat.pmp.Filter;
import dev.blackilykat.pmp.Order;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains a list of all sessions that currently exist. When the server sends this, clients must ensure their session
 * list is identical to the one contained in this message, whether that's by removing, adding or modifying its existing
 * session list. If clients need a session to keep existing regardless of whether it was previously on the server, they
 * can keep it and send a {@link PlaybackSessionCreateMessage}, ensuring there is no mismatching information between the
 * client and the server.
 */
public class PlaybackSessionListMessage extends Message {
	public static final String MESSAGE_TYPE = "PlaybackSessionList";

	public List<PlaybackSessionElement> sessions = new ArrayList<>();

	public PlaybackSessionListMessage(Collection<PlaybackSessionElement> sessions) {
		this.sessions.addAll(sessions);
	}

	public static class PlaybackSessionElement {
		public Integer id;
		public String track;
		public ShuffleOption shuffle;
		public RepeatOption repeat;
		public Boolean playing;
		public Integer lastPositionUpdate;
		public Integer owner;
		public Instant lastUpdateTime;
		public List<Filter> filters;
		public Integer sortingHeader;
		public Order sortingOrder;

		public PlaybackSessionElement(Integer id, String track, ShuffleOption shuffle, RepeatOption repeat,
				Boolean playing, Integer lastPositionUpdate, Integer owner, List<Filter> filters,
				Integer sortingHeader,
				Order sortingOrder, Instant lastUpdateTime) {
			this.id = id;
			this.track = track;
			this.shuffle = shuffle;
			this.repeat = repeat;
			this.playing = playing;
			this.lastPositionUpdate = lastPositionUpdate;
			this.owner = owner;
			this.filters = filters;
			this.lastUpdateTime = lastUpdateTime;
			this.sortingHeader = sortingHeader;
			this.sortingOrder = sortingOrder;
		}
	}
}
