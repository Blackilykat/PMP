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

package dev.blackilykat.pmp.event;

/// An event source which stores the last event and calls newly registered
/// listeners if a previous event was emitted.
///
/// Useful for event sources that expose state to UI, to avoid code duplication
/// in the UI layer.
public class RetroactiveEventSource<T> extends EventSource<T> {
	private T lastEvent = null;

	@Override
	public synchronized void register(Listener<T> listener) {
		super.register(listener);

		if(lastEvent != null) {
			listener.run(lastEvent);
		}
	}

	@Override
	public synchronized void call(T t) {
		super.call(t);

		lastEvent = t;
	}
}
