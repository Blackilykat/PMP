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

public class StoredList<T> extends Stored<List<T>> {
	public StoredList(Type type, Storage storage) {
		super(new ParType(List.class, new Type[]{type}), storage, new LinkedList<>());
	}

	public StoredList(Type type, Storage storage, List<T> initial) {
		super(new ParType(List.class, new Type[]{type}), storage, new LinkedList<>(initial));
	}

	@Override
	public void set(List<T> newValue) {
		super.set(new LinkedList<>(newValue));
	}

	@Override
	public List<T> get() {
		return Collections.unmodifiableList(super.get());
	}

	public void add(T object) {
		synchronized(storage) {
			storage.markDirty();
			value.add(object);
		}
	}

	public void add(int position, T object) {
		synchronized(storage) {
			storage.markDirty();
			value.add(position, object);
		}
	}

	public void remove(T object) {
		synchronized(storage) {
			storage.markDirty();
			value.remove(object);
		}
	}

	public int size() {
		synchronized(storage) {
			return value.size();
		}
	}

	public boolean empty() {
		return size() == 0;
	}

	public T get(int i) {
		synchronized(storage) {
			return super.get().get(i);
		}
	}

	public T getFirst() {
		return get(0);
	}

	public T getLast() {
		return get(size() - 1);
	}

	public boolean contains(T object) {
		synchronized(storage) {
			return value.contains(object);
		}
	}

	public int indexOf(T object) {
		synchronized(storage) {
			return value.indexOf(object);
		}
	}
}
