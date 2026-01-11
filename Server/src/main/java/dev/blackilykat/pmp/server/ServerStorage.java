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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ServerStorage {
	private static final Logger LOGGER = LogManager.getLogger(ServerStorage.class);
	public static Main MAIN;
	public static Sensitive SENSITIVE;

	public static void load() throws IOException {
		MAIN = Storage.load(Main.NAME, Main.class);
		SENSITIVE = Storage.load(Sensitive.NAME, Sensitive.class);
	}

	public static class Main extends Storage {
		private static final String NAME = "server";

		public final StoredInt currentDeviceID = new StoredInt(this, 0);
		public final StoredMap<String, Track> tracks = new StoredMap<>(String.class, Track.class, this);

		// index = action ID
		public final StoredList<Action> actions = new StoredList<>(Action.class, this);

		public final Stored<String> track = new Stored<>(String.class, this, null);
		public final StoredList<Pair<Integer, String>> positiveFilterOptions = new StoredList<>(
				new ParType(Pair.class, new Type[]{Integer.class, String.class}), this);
		public final StoredList<Pair<Integer, String>> negativeFilterOptions = new StoredList<>(
				new ParType(Pair.class, new Type[]{Integer.class, String.class}), this);
		public final Stored<RepeatOption> repeat = new Stored<>(RepeatOption.class, this, RepeatOption.ALL);
		public final Stored<ShuffleOption> shuffle = new Stored<>(ShuffleOption.class, this, ShuffleOption.OFF);
		public final Stored<Long> position = new Stored<>(Long.class, this, 0L);
		public final StoredList<FilterInfo> filters = new StoredList<>(FilterInfo.class, this);

		public Main() {
			super(NAME);
		}
	}

	public static class Sensitive extends SensitiveStorage {
		private static final String NAME = ".sensitive_server";
		public final StoredList<Device> devices = new StoredList<>(Device.class, this);
		public final Stored<String> password = new Stored<>(String.class, this, null);

		public Sensitive() {
			super(NAME);
		}
	}

	public record PlaybackState(String track, Long position, ShuffleOption shuffle, RepeatOption repeat,
			List<Pair<Integer, String>> positiveOptions, List<Pair<Integer, String>> negativeOptions) {

		public PlaybackState(String track, Long position, ShuffleOption shuffle, RepeatOption repeat,
				List<Pair<Integer, String>> positiveOptions, List<Pair<Integer, String>> negativeOptions) {
			this.track = track;
			this.position = position;
			this.shuffle = shuffle;
			this.repeat = repeat;
			this.positiveOptions = Collections.unmodifiableList(new LinkedList<>(positiveOptions));
			this.negativeOptions = Collections.unmodifiableList(new LinkedList<>(negativeOptions));
		}
	}
}
