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
import dev.blackilykat.pmp.FilterInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * Update the list of filters. Only contains id and key of the filter: the option states are in
 * {@link PlaybackUpdateMessage}. Clients should use the IDs to keep existing filters with previous states, and add new
 * filters if new. This may not be followed by a PlaybackUpdate if there is no owner.
 * <p>Direction: Bidirectional (C2S, C2S2C)
 */
public class FilterListMessage extends Message {
	public static final String MESSAGE_TYPE = "FilterList";

	public List<FilterInfo> filters;

	public FilterListMessage() {
		filters = new LinkedList<>();
	}

	@JsonCreator
	public FilterListMessage(List<FilterInfo> filters) {
		this.filters = filters;
	}
}
