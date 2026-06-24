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

/// Generic stored map for ease of use.
public class StoredMap<K, V> extends Stored<Map<K, V>> {
	/// Create a stored map with an empty initial value.
	public StoredMap(Type keyType, Type valueType, Storage storage) {
		super(new ParType(HashMap.class, new Type[]{keyType, valueType}), storage, new HashMap<>());
	}

	/// Set the value to a copy of the given map and mark storage as dirty.
	@Override
	public void set(Map<K, V> newValue) {
		super.set(new HashMap<>(newValue));
	}

	/// Get an unmodifiable view of the map.
	@Override
	public Map<K, V> get() {
		return Collections.unmodifiableMap(super.get());
	}

	/// Remove the given key from the map and mark storage as dirty.
	public void remove(K key) {
		synchronized(storage) {
			value.remove(key);
			storage.markDirty();
		}
	}

	/// Put the given key and value in the map and mark storage as dirty.
	public void put(K key, V value) {
		synchronized(storage) {
			storage.markDirty();
			this.value.put(key, value);
		}
	}

	/// Get the value for the given key from the map.
	public V get(K key) {
		synchronized(storage) {
			return value.get(key);
		}
	}

	/// Returns `true` if the map contains a mapping for the specified key.
	/// @return `true` if the map contains a mapping for the specified key.
	public boolean containsKey(K key) {
		synchronized(storage) {
			return value.containsKey(key);
		}
	}

	/// Returns `true` if the map contains a mapping with the specified value.
	/// @return `true` if the map contains a mapping with the specified value.
	public boolean containsValue(V value) {
		synchronized(storage) {
			return this.value.containsValue(value);
		}
	}

	/// Returns a view of all values in the map.
	/// @return a view of all values in the map.
	public Collection<V> values() {
		synchronized(storage) {
			return value.values();
		}
	}
}
