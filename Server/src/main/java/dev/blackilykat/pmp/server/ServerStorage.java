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

package dev.blackilykat.pmp.server;

import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.storage.SensitiveStorage;
import dev.blackilykat.pmp.storage.Storage;
import dev.blackilykat.pmp.storage.Stored;
import dev.blackilykat.pmp.storage.StoredInt;
import dev.blackilykat.pmp.storage.StoredList;
import dev.blackilykat.pmp.storage.StoredMap;
import dev.blackilykat.pmp.util.Pair;
import dev.blackilykat.pmp.util.ParType;

import java.io.IOException;
import java.lang.reflect.Type;

/// All storage used in the server.
///
/// @see Main
/// @see Sensitive
public class ServerStorage {
	public static Main MAIN;
	public static Sensitive SENSITIVE;

	public static void load() throws IOException {
		MAIN = Storage.load(Main.NAME, Main.class);
		SENSITIVE = Storage.load(Sensitive.NAME, Sensitive.class);
	}

	/// Main storage used in the server. Contains general information, mostly related to playback and filters.
	public static class Main extends Storage {
		private static final String NAME = "server";

		/// Incremental counter used to assign [Device] [Device#id]s.
		public final StoredInt currentDeviceID = new StoredInt(this, 0);

		/// All tracks in the server's library. Acts as a cache while initializing.
		public final StoredMap<String, Track> tracks = new StoredMap<>(String.class, Track.class, this);

		/// All actions ever performed on the library. Their index is equivalent to the action ID.
		public final StoredList<Action> actions = new StoredList<>(Action.class, this);

		/// The currently playing track's filename.
		public final Stored<String> track = new Stored<>(String.class, this, null);

		/// All filter options selected [dev.blackilykat.pmp.client.FilterOption.State#POSITIVE]ly.
		///
		/// The pair contains filter [dev.blackilykat.pmp.client.Filter#id] and option
		/// [dev.blackilykat.pmp.client.FilterOption#value].
		public final StoredList<Pair<Integer, String>> positiveFilterOptions = new StoredList<>(
				new ParType(Pair.class, new Type[]{Integer.class, String.class}), this);

		/// All filter options selected [dev.blackilykat.pmp.client.FilterOption.State#NEGATIVE]ly.
		///
		/// The pair contains filter [dev.blackilykat.pmp.client.Filter#id] and option
		/// [dev.blackilykat.pmp.client.FilterOption#value].
		public final StoredList<Pair<Integer, String>> negativeFilterOptions = new StoredList<>(
				new ParType(Pair.class, new Type[]{Integer.class, String.class}), this);

		/// State of playback repeat.
		public final Stored<RepeatOption> repeat = new Stored<>(RepeatOption.class, this, RepeatOption.ALL);
		/// State of playback shuffle.
		public final Stored<ShuffleOption> shuffle = new Stored<>(ShuffleOption.class, this, ShuffleOption.OFF);
		/// Position in the playing track in milliseconds. Only updated when saving.
		public final Stored<Long> position = new Stored<>(Long.class, this, 0L);

		// Basic information on filters.
		public final StoredList<FilterInfo> filters = new StoredList<>(FilterInfo.class, this);

		public Main() {
			super(NAME);
		}
	}

	/// Sensitive server storage, contains private values and values which must be stored as quickly as possible.
	public static class Sensitive extends SensitiveStorage {
		private static final String NAME = ".sensitive_server";
		/// All known devices. Sensitive because this must be saved immediately upon update, and
		/// because it contains tokens.
		public final StoredList<Device> devices = new StoredList<>(Device.class, this);
		/// The password the user must enter to log in with a new device.
		public final Stored<String> password = new Stored<>(String.class, this, null);

		public Sensitive() {
			super(NAME);
		}
	}
}
