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

package dev.blackilykat.pmp.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * An event source is the object where you can register and call event listeners.
 *
 * @param <T> The type of the event. Creating a static inner class (or record) with the data needed for the listeners is
 * encouraged if you need to have more values or if it isn't immediately clear what a value may represent. For simple
 * listeners, it is fine to use any class here.
 */
public class EventSource<T> {
	private static final Logger LOGGER = LogManager.getLogger(EventSource.class);
	private List<Listener<T>> listeners = new LinkedList<>();

	public void register(Listener<T> listener) {
		listeners.add(listener);
	}

	public void unregister(Listener<T> listener) {
		if(!listeners.remove(listener)) {
			LOGGER.warn("Removed event listener twice", new Throwable());
		}
	}

	public void call(T t) {
		// Calling toArray is necessary so that you can call unregister() inside a listener

		//noinspection unchecked
		for(Listener<T> listener : listeners.toArray(new Listener[0])) {
			listener.run(t);
		}
	}
}
