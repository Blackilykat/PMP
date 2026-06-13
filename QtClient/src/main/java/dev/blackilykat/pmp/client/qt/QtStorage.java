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

package dev.blackilykat.pmp.client.qt;

import java.io.IOException;

import dev.blackilykat.pmp.storage.Storage;
import dev.blackilykat.pmp.storage.StoredMap;

class QtStorage {
	public static Main MAIN;

	public static void load() throws IOException {
		MAIN = Storage.load(Main.NAME, Main.class);
	}

	public static class Main extends Storage {
		public static final String NAME = "qt";

		public final StoredMap<Integer, Integer> headerWidths = new StoredMap<>(Integer.class, Integer.class, this);

		public Main() {
			super(NAME);
		}
	}
}
