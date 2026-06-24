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

package dev.blackilykat.pmp.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.blackilykat.pmp.event.EventSource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

/// Library filters are the tool offered to allow narrowing the library down to only desired tracks.
///
/// Think of it as a sort of "dynamic playlist", which selects track through common traits instead of manually.
///
/// Filters are composed by a [#key], equivalent to FLAC metadata keys, and [#options], which are all the
/// unique value for the specified key.
///
/// Filters are applied in order.
/// This means that the first filter will work with all tracks in the library while the last filter will
/// only have [#options] found in the tracks which match all previous filters.
///
/// @see FilterOption.State
public class Filter {
	/// String used in filter options to represent matching every track for the filter.
	public static final String OPTION_EVERYTHING = "__PMP_OPTION_EVERYTHING__";
	/// String used in filter options to represent matching tracks which do not have the metadata
	/// described by the filter.
	public static final String OPTION_UNKNOWN = "__PMP_OPTION_UNKNOWN__";

	/// Emitted when a filter option has been added to any filter. For updating UI.
	///
	/// @see #eventOptionAdded
	public static final EventSource<OptionAddedEvent> EVENT_OPTION_ADDED = new EventSource<>();
	/// Emitted when a filter option has been removed from any filters. For updating UI.
	///
	/// @see #eventOptionRemoved
	public static final EventSource<OptionRemovedEvent> EVENT_OPTION_REMOVED = new EventSource<>();
	/// Emitted when a filter option has changed its selected state. For updating UI.
	///
	/// @see FilterOption#eventChangedState
	public static final EventSource<OptionChangedStateEvent> EVENT_OPTION_CHANGED_STATE = new EventSource<>();

	/// Emitted when this filter option has been added to any filter. For updating UI.
	///
	/// @see #EVENT_OPTION_ADDED
	public final EventSource<OptionAddedEvent> eventOptionAdded = new EventSource<>();
	/// Emitted when this filter option has been removed from any filters. For updating UI.
	///
	/// @see #EVENT_OPTION_REMOVED
	public final EventSource<OptionRemovedEvent> eventOptionRemoved = new EventSource<>();

	/// The ID of this filter. Used to allow simple communication of filter option state updates,
	/// and non-jank operations on filters such as reordering while maintaining state.
	public final int id;

	/// The actual options in this filter, along with their state. Used during runtime, not
	/// stored in this object.
	@JsonIgnore
	private final List<FilterOption> options = new LinkedList<>();

	/// The metadata key by which tracks will be filtered. Must be an exact case-insensitive match
	/// to metadata found in FLAC files.
	public @Nonnull String key;

	public Filter(@Nonnull String key) {
		this(ClientStorage.MAIN.currentFilterID.getAndIncrement(), key);
	}

	@JsonCreator
	public Filter(int id, @Nonnull String key) {
		this.id = id;
		this.key = key;
	}

	/// Add a filter option to this filter.
	///
	/// Places it in alphabetical order, while keeping [#OPTION_EVERYTHING] first and [#OPTION_UNKNOWN] last.
	///
	/// @throws IllegalArgumentException if this option has been added to a different filter.
	/// @see #EVENT_OPTION_ADDED
	/// @see #eventOptionAdded
	public void addOption(@Nonnull FilterOption option) {
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

	/// Remove an option from this filter.
	///
	/// @throws IllegalArgumentException if this option was not in this filter.
	/// @see #EVENT_OPTION_REMOVED
	/// @see #eventOptionRemoved
	public void removeOption(@Nonnull FilterOption option) {
		if(option.parent != this) {
			throw new IllegalArgumentException("Option is not owned by this filter");
		}

		option.parent = null;
		options.remove(option);

		OptionRemovedEvent evt = new OptionRemovedEvent(this, option);
		EVENT_OPTION_REMOVED.call(evt);
		eventOptionRemoved.call(evt);
	}

	/// Get an unmodifiable view of this filter's options.
	public List<FilterOption> getOptions() {
		return Collections.unmodifiableList(options);
	}

	/// Add and remove the necessary options to make the list of options match the given list.
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

	@Override
	public String toString() {
		return "Filter(" + id + ", " + key + ")";
	}

	/// Data for [Filter#EVENT_OPTION_ADDED] and [Filter#eventOptionAdded].
	public record OptionAddedEvent(Filter filter, int index, FilterOption option) {}

	/// Data for [Filter#EVENT_OPTION_REMOVED] and [Filter#eventOptionRemoved].
	public record OptionRemovedEvent(Filter filter, FilterOption option) {}

	/// Data for [Filter#EVENT_OPTION_CHANGED_STATE] and [FilterOption#eventChangedState].
	public record OptionChangedStateEvent(Filter filter, FilterOption option, FilterOption.State oldState,
			FilterOption.State newState) {}
}
