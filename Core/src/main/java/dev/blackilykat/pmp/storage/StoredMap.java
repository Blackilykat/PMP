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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StoredMap<K, V> extends Stored<Map<K, V>> {
	public StoredMap(Type keyType, Type valueType, Storage storage) {
		super(new ParType(HashMap.class, new Type[]{keyType, valueType}), storage, new HashMap<>());
	}

	@Override
	public void set(Map<K, V> newValue) {
		super.set(new HashMap<>(newValue));
	}

	@Override
	public Map<K, V> get() {
		return Collections.unmodifiableMap(super.get());
	}

	public void remove(K key) {
		synchronized(storage) {
			storage.markDirty();
			value.remove(key);
		}
	}

	public void put(K key, V value) {
		synchronized(storage) {
			storage.markDirty();
			this.value.put(key, value);
		}
	}

	public V get(K key) {
		synchronized(storage) {
			return value.get(key);
		}
	}

	public boolean containsKey(K key) {
		synchronized(storage) {
			return value.containsKey(key);
		}
	}

	public boolean containsValue(V value) {
		synchronized(storage) {
			return this.value.containsValue(value);
		}
	}

	public Collection<V> values() {
		synchronized(storage) {
			return value.values();
		}
	}
}
