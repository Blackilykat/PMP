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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.lang.reflect.Type;

/// A value which is stored in a [Storage] file.
///
/// This class is necessary to consistently synchronize reads and writes from and to storage,
/// and to allow all updates to stored values to always mark storage as dirty.
public class Stored<T> {
	public final Type type;
	protected final Storage storage;
	protected T value;

	public Stored(Type type, Storage storage, T initial) {
		this.type = type;
		this.storage = storage;
		this.value = initial;
	}

	/// Set the inner stored value and mark storage as dirty.
	public void set(T newValue) {
		synchronized(storage) {
			value = newValue;
			storage.markDirty();
		}
	}

	/// Get the inner stored value.
	public T get() {
		synchronized(storage) {
			return value;
		}
	}

	/// Jackson serializer which allows stored values to not be wrapped in unnecessary [Stored] objects.
	public static class Serializer extends StdSerializer<Stored<?>> {
		protected Serializer() {
			//noinspection unchecked
			super((Class<Stored<?>>) (Class<?>) Stored.class);
		}

		@Override
		public void serialize(Stored<?> stored, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
				throws IOException {
			jsonGenerator.writeObject(stored.value);
		}
	}
}

