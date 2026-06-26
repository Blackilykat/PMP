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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/// An event source is the object where you can register and call event listeners.
///
/// Listeners will be called on whatever thread the event was invoked in.
/// You must therefore keep thread safety in mind when writing listeners.
///
/// Static event names should be prefixed with `EVENT_`, i.e. `EVENT_PLAY_PAUSE`, `EVENT_TRACK_ADDED`.
/// Non static event names should be prefixed with `event`, i.e. `eventDisconnected`, `eventSent`.
///
/// Listeners can and should unregister themselves if they were designed to work up to a certain point (see
/// [Listener#registerOneTime]).
///
/// @param <T> The type of the event. Creating a static inner class (or record) with the data needed for the listeners is
/// encouraged if you need to have more values or if it isn't immediately clear what a value may represent. For simple
/// listeners, it is fine to use any class here.
///
/// @see Listener
@JsonIgnoreType
public class EventSource<T> {
	private static final Logger LOGGER = LogManager.getLogger(EventSource.class);

	/// The registered listeners to this source
	private List<Listener<T>> listeners = new LinkedList<>();

	/// Register a listener to this source.
	///
	/// @see #unregister
	/// @see EventSource
	public void register(Listener<T> listener) {
		listeners.add(listener);
	}

	/// Unregister a listener to this source.
	///
	/// @see #register
	/// @see EventSource
	public void unregister(Listener<T> listener) {
		if(!listeners.remove(listener)) {
			LOGGER.warn("Removed event listener twice", new Throwable());
		}
	}

	/// Call all registered listeners for this event source.
	///
	/// @throws RuntimeException if any listener throws an exception.
	public void call(T t) {
		// Calling toArray is necessary so that you can call unregister() inside a listener

		//noinspection unchecked
		for(Listener<T> listener : listeners.toArray(new Listener[0])) {
			listener.run(t);
		}
	}
}
