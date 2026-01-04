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

public abstract class Storage {

	private static Storage storage = null;

	public int getAndIncrementCurrentFilterId() {
		int id = getCurrentFilterID();
		setCurrentFilterID(id + 1);
		return id;
	}

	public abstract int getCurrentFilterID();

	public abstract void setCurrentFilterID(int id);

	public int getAndIncrementCurrentSessionId() {
		int id = getCurrentSessionID();
		setCurrentSessionID(id + 1);
		return id;
	}

	public abstract int getCurrentSessionID();

	public abstract void setCurrentSessionID(int id);

	public static Storage getStorage() {
		if(storage == null) {
			throw new IllegalStateException("Storage was never set");
		}
		return storage;
	}

	public static void setStorage(Storage value) {
		storage = value;
	}
}
