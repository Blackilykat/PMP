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

package dev.blackilykat.pmp.storage;

import dev.blackilykat.pmp.util.ParType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/// Generic stored list for ease of use.
public class StoredList<T> extends Stored<List<T>> {
	/// Create a stored list with an empty list as default.
	public StoredList(Type type, Storage storage) {
		super(new ParType(List.class, new Type[]{type}), storage, new LinkedList<>());
	}

	/// Create a stored list with a specified default.
	public StoredList(Type type, Storage storage, List<T> initial) {
		super(new ParType(List.class, new Type[]{type}), storage, new LinkedList<>(initial));
	}

	/// Set the value to a copy of the given list and mark storage as dirty.
	@Override
	public void set(List<T> newValue) {
		super.set(new LinkedList<>(newValue));
	}

	/// Get an unmodifiable view of the value.
	@Override
	public List<T> get() {
		return Collections.unmodifiableList(super.get());
	}

	/// Add `object` to the list and mark storage as dirty.
	public void add(T object) {
		synchronized(storage) {
			value.add(object);
			storage.markDirty();
		}
	}

	/// Add `object` at index `position` to the list and mark storage as dirty.
	public void add(int position, T object) {
		synchronized(storage) {
			value.add(position, object);
			storage.markDirty();
		}
	}

	/// Remove `object` from the list and mark storage as dirty.
	public void remove(T object) {
		synchronized(storage) {
			value.remove(object);
			storage.markDirty();
		}
	}

	/// Returns the amount of elements in the list.
	public int size() {
		synchronized(storage) {
			return value.size();
		}
	}

	/// Returns true if the list is empty.
	public boolean empty() {
		return size() == 0;
	}

	/// Returns the element at the specified position the list.
	/// @param i index of the element to return
	/// @throws IndexOutOfBoundsException if the index is out of range
	public T get(int i) {
		synchronized(storage) {
			return super.get().get(i);
		}
	}

	/// Returns the first element of the list.
	///
	/// Equivalent to [#get]`(0)`.
	///
	/// @throws IndexOutOfBoundsException if the list is empty
	public T getFirst() {
		return get(0);
	}

	/// Returns the last element of the list.
	///
	/// Equivalent to [#get]`(`[#size]`() - 1)`.
	///
	/// @throws IndexOutOfBoundsException if the list is empty
	public T getLast() {
		return get(size() - 1);
	}

	/// Returns true if the list contains the specified object.
	public boolean contains(T object) {
		synchronized(storage) {
			return value.contains(object);
		}
	}

	/// Returns the index of the specified object in the list, or -1 if the list does not contain it.
	public int indexOf(T object) {
		synchronized(storage) {
			return value.indexOf(object);
		}
	}
}
