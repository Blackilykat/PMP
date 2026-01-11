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

package dev.blackilykat.pmp.client;

import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.storage.SensitiveStorage;
import dev.blackilykat.pmp.storage.Storage;
import dev.blackilykat.pmp.storage.Stored;
import dev.blackilykat.pmp.storage.StoredBlockingDeque;
import dev.blackilykat.pmp.storage.StoredInt;
import dev.blackilykat.pmp.storage.StoredKey;
import dev.blackilykat.pmp.storage.StoredList;
import dev.blackilykat.pmp.storage.StoredMap;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class ClientStorage {
	private static final Logger LOGGER = LogManager.getLogger(ClientStorage.class);
	public static Main MAIN;
	public static Sensitive SENSITIVE;

	public static void load() throws IOException {
		MAIN = Storage.load(Main.NAME, Main.class);
		SENSITIVE = Storage.load(Sensitive.NAME, Sensitive.class);
	}

	public static class Main extends Storage {
		private static final String NAME = "client";
		public StoredMap<String, Track> tracks = new StoredMap<>(String.class, Track.class, this);
		public StoredList<Header> headers = new StoredList<>(Header.class, this);
		public StoredList<Filter> filters = new StoredList<>(Filter.class, this);
		public StoredInt currentFilterID = new StoredInt(this, 0);
		public StoredInt currentHeaderID = new StoredInt(this, 0);
		public Stored<String> serverAddress = new Stored<>(String.class, this, "localhost");
		public StoredInt serverPort = new StoredInt(this, PMPConnection.DEFAULT_MESSAGE_PORT);
		public StoredInt serverFilePort = new StoredInt(this, PMPConnection.DEFAULT_FILE_PORT);
		public Stored<PlaybackInfo> playbackInfo = new Stored<>(PlaybackInfo.class, this, null);
		public StoredList<FilterInfo> lastKnownServerFilters = new StoredList<>(FilterInfo.class, this);
		public StoredBlockingDeque<Action> actionsToHandle = new StoredBlockingDeque<>(Action.class, this);
		public StoredInt lastReceivedAction = new StoredInt(this, -1);
		public StoredBlockingDeque<Action> actionsToSend = new StoredBlockingDeque<>(Action.class, this);

		private Main() {
			super(NAME);
		}
	}

	public static class Sensitive extends SensitiveStorage {
		private static final String NAME = ".sensitive";
		public Stored<Integer> deviceID = new Stored<>(Integer.class, this, null);
		public Stored<String> token = new Stored<>(String.class, this, null);
		public StoredKey serverPublicKey = new StoredKey(this);

		private Sensitive() {
			super(NAME);
		}
	}

	// playing is not here because it should never be playing at startup
	public record PlaybackInfo(String track, long position, List<Pair<Integer, String>> positiveFilterOptions,
			List<Pair<Integer, String>> negativeFilterOptions, RepeatOption repeat, ShuffleOption shuffle) {}
}
