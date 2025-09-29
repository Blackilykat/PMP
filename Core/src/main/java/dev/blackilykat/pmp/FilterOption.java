
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

import dev.blackilykat.pmp.event.EventSource;

public class FilterOption {
	public final String value;
	public final EventSource<Filter.OptionChangedStateEvent> eventChangedState = new EventSource<>();
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

	public void setState(State state) {
		State oldState = this.state;
		this.state = state;

		Filter.OptionChangedStateEvent evt = new Filter.OptionChangedStateEvent(parent, this, oldState, state);
		Filter.EVENT_OPTION_CHANGED_STATE.call(evt);
		eventChangedState.call(evt);
	}

	public enum State {
		NONE, POSITIVE, NEGATIVE
	}
}
