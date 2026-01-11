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

public class StoredInt extends Stored<Integer> {
	public StoredInt(Storage storage, int initial) {
		super(Integer.class, storage, initial);
	}

	@Override
	public void set(Integer newValue) {
		if(newValue == null) {
			throw new IllegalArgumentException("StoredInt does not support null values");
		}
		super.set(newValue);
	}

	public int getAndIncrement() {
		synchronized(storage) {
			storage.markDirty();
			return value++;
		}
	}

	public void increment() {
		synchronized(storage) {
			storage.markDirty();
			value++;
		}
	}
}
