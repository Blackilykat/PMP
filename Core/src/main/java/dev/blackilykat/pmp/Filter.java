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

package dev.blackilykat.pmp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.blackilykat.pmp.event.EventSource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Filter {
	public static final String OPTION_EVERYTHING = "__PMP_OPTION_EVERYTHING__";
	public static final String OPTION_UNKNOWN = "__PMP_OPTION_UNKNOWN__";

	public static final EventSource<OptionAddedEvent> EVENT_OPTION_ADDED = new EventSource<>();
	public static final EventSource<OptionRemovedEvent> EVENT_OPTION_REMOVED = new EventSource<>();
	public static final EventSource<OptionChangedStateEvent> EVENT_OPTION_CHANGED_STATE = new EventSource<>();

	public final EventSource<OptionAddedEvent> eventOptionAdded = new EventSource<>();
	public final EventSource<OptionRemovedEvent> eventOptionRemoved = new EventSource<>();

	public final int id;
	public final String key;

	@JsonIgnore
	private final List<FilterOption> options = new LinkedList<>();

	public Filter(String key) {
		this(Storage.getStorage().getAndIncrementCurrentFilterId(), key);
	}

	@JsonCreator
	public Filter(int id, String key) {
		this.id = id;
		this.key = key;
	}

	public void addOption(FilterOption option) {
		if(option.parent != null) {
			throw new IllegalArgumentException("Option is owned by a different filter");
		}


		int index = 0;
		if(option.value.equals(OPTION_UNKNOWN)) {
			index = options.size();
		} else if(!option.value.equals(OPTION_EVERYTHING)) {
			for(FilterOption existingOption : options) {
				if(existingOption.value.equals(OPTION_UNKNOWN)) {
					break;
				}

				if(existingOption.value.equals(OPTION_EVERYTHING)) {
					index++;
					continue;
				}

				int comparison = existingOption.value.compareToIgnoreCase(option.value);
				if(comparison == 0) {
					throw new IllegalArgumentException("This option already exists");
				}

				if(comparison > 0) {
					break;
				}

				index++;
			}
		}

		option.parent = this;
		options.add(index, option);

		OptionAddedEvent evt = new OptionAddedEvent(this, index, option);
		EVENT_OPTION_ADDED.call(evt);
		eventOptionAdded.call(evt);
	}

	public void removeOption(FilterOption option) {
		if(option.parent != this) {
			throw new IllegalArgumentException("Option is not owned by this filter");
		}

		option.parent = null;
		options.remove(option);

		OptionRemovedEvent evt = new OptionRemovedEvent(this, option);
		EVENT_OPTION_REMOVED.call(evt);
		eventOptionRemoved.call(evt);
	}

	public List<FilterOption> getOptions() {
		return Collections.unmodifiableList(options);
	}

	public void applyOptionValues(List<String> values) {
		for(String value : values) {
			boolean found = false;
			for(FilterOption option : options) {
				if(value.equalsIgnoreCase(option.value)) {
					found = true;
					break;
				}
			}

			if(!found) {
				addOption(new FilterOption(value));
			}
		}

		for(FilterOption option : options.toArray(new FilterOption[0])) {
			boolean found = false;
			for(String value : values) {
				if(value.equals(option.value)) {
					found = true;
					break;
				}
			}

			if(!found) {
				removeOption(option);
			}
		}
	}

	public record OptionAddedEvent(Filter filter, int index, FilterOption option) {}

	public record OptionRemovedEvent(Filter filter, FilterOption option) {}

	public record OptionChangedStateEvent(Filter filter, FilterOption option, FilterOption.State oldState,
			FilterOption.State newState) {}
}
