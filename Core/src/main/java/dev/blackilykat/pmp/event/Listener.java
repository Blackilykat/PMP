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

import java.util.concurrent.atomic.AtomicReference;

/// Event listeners used to listen to [EventSource]s.
public interface Listener<T> {
	void run(T event);


	/// Creates a listener which unregisters itself after being called once
	public static <T> void registerOneTime(EventSource<T> source, Listener<T> listener) {
		AtomicReference<Listener<T>> ref = new AtomicReference<>();
		ref.set(t -> {
			listener.run(t);
			source.unregister(ref.get());
		});
		source.register(ref.get());
	}
}
