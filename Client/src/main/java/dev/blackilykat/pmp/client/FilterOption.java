
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

package dev.blackilykat.pmp.client;

import dev.blackilykat.pmp.event.EventSource;

/// An option in a filter, represents a unique metadata value for the filter's key.
public class FilterOption {
	/// The metadata value of this option.
	///
	/// Will match all tracks which contain this exact metadata value with the [#parent]'s [Filter#key].
	///
	/// Case-insensitive.
	public final String value;

	/// Emitted when this filter option has changed its selected state. For updating UI.
	///
	/// @see Filter#EVENT_OPTION_CHANGED_STATE
	public final EventSource<Filter.OptionChangedStateEvent> eventChangedState = new EventSource<>();

	/// The filter this option is in.
	protected Filter parent = null;

	private State state = State.NONE;

	public FilterOption(String value) {
		this.value = value;
	}

	public Filter getParent() {
		return parent;
	}

	public State getState() {
		return state;
	}

	/// @see Filter#EVENT_OPTION_CHANGED_STATE
	/// @see #eventChangedState
	public void setState(State state) {
		State oldState = this.state;
		this.state = state;

		Filter.OptionChangedStateEvent evt = new Filter.OptionChangedStateEvent(parent, this, oldState, state);
		Filter.EVENT_OPTION_CHANGED_STATE.call(evt);
		eventChangedState.call(evt);
	}

	/// Possible selection states of an option.
	public enum State {
		/// This option will not cause any track to match, but will not prevent any track from matching either.
		NONE,
		/// All tracks with a metadata entry which matches the [#parent]'s [Filter#key] and [#value] (both case-insensitive)
		/// will match, unless they also match an option with [#NEGATIVE] state.
		POSITIVE,
		/// All tracks with a metadata entry which matches the [#parent]'s [Filter#key] and [#value] (both case-insensitive)
		/// will **not** match, even if they match another option with [#POSITIVE] state.
		NEGATIVE
	}
}
