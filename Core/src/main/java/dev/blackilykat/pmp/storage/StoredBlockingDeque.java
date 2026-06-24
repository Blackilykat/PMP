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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/// [BlockingDeque] for use in storage designed to have at most 1 consuming thread.
public class StoredBlockingDeque<T> extends Stored<BlockingDeque<T>> {
	public StoredBlockingDeque(Type type, Storage storage) {
		super(new ParType(LinkedBlockingDeque.class, new Type[]{type}), storage, new LinkedBlockingDeque<>());
	}

	// set must be allowed to let the json deserializer use it
	@Override
	public BlockingDeque<T> get() {
		assert false;
		throw new RuntimeException("Do not use get on a stored queue");
	}

	/// Equivalent to [#peek], but if the queue is empty blocks until a value is added.
	public T blockingPeek() throws InterruptedException {
		T peeked = value.take();
		value.addFirst(peeked);
		return peeked;
	}

	/// Equivalent to [BlockingDeque#peek]
	public T peek() {
		synchronized(storage) {
			return value.peek();
		}
	}

	/// Equivalent to [BlockingDeque#add]
	public void add(T t) {
		synchronized(storage) {
			value.add(t);
			storage.markDirty();
		}
	}

	/// Equivalent to [BlockingDeque#take]
	public void take() {
		value.remove();
		storage.markDirty();
	}

	/// Get a safely iterable copy of the list of all values in this queue.
	public List<T> viewAll() {
		synchronized(storage) {
			return new ArrayList<>(value);
		}
	}
}
