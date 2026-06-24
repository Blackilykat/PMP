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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.messages.ActionResponse;
import dev.blackilykat.pmp.messages.GetActionsRequest;
import dev.blackilykat.pmp.messages.LoginAsExistingDeviceRequest;
import dev.blackilykat.pmp.messages.LoginSuccessResponse;
import dev.blackilykat.pmp.storage.SensitiveStorage;
import dev.blackilykat.pmp.storage.Storage;
import dev.blackilykat.pmp.storage.Stored;
import dev.blackilykat.pmp.storage.StoredBlockingDeque;
import dev.blackilykat.pmp.storage.StoredInt;
import dev.blackilykat.pmp.storage.StoredKey;
import dev.blackilykat.pmp.storage.StoredList;
import dev.blackilykat.pmp.storage.StoredMap;
import dev.blackilykat.pmp.util.Pair;

import java.io.IOException;
import java.util.List;

/// All client storage files.
///
/// @see Main
/// @see Sensitive
public class ClientStorage {
	/// The main client storage file. Contains general state that is expected to stay between restarts.
	public static Main MAIN;
	/// The client sensitive file. Contains login information that must be saved immediately including the private token.
	public static Sensitive SENSITIVE;

	/// Load [#MAIN] and [#SENSITIVE] from disk.
	public static void load() throws IOException {
		MAIN = Storage.load(Main.NAME, Main.class);
		SENSITIVE = Storage.load(Sensitive.NAME, Sensitive.class);
	}

	/// The main client storage file. Contains general state that is expected to stay between restarts.
	public static class Main extends Storage {
		private static final String NAME = "client";
		/// Information and metadata about all tracks int he library.
		///
		/// Acts as a cache: this can be rebuilt if storage fails to load, but takes a long time with
		/// large libraries due to the need to calculate a checksum of each entire file.
		public StoredMap<String, Track> tracks = new StoredMap<>(String.class, Track.class, this);

		/// All track headers.
		///
		/// In some interfaces this is the data displayed about each track.
		///
		/// In all interfaces, these are the values tracks can be sorted by.
		public StoredList<Header> headers = new StoredList<>(Header.class, this);

		/// Ids and keys of filters. Does not contain filter options (the value is annotated with [JsonIgnore]):
		/// options are contained in [#playbackInfo].
		public StoredList<Filter> filters = new StoredList<>(Filter.class, this);

		/// Current filter ID. To be removed soon, see issue #20.
		public StoredInt currentFilterID = new StoredInt(this, 0);

		/// Current header ID. To be removed soon, see issue #20.
		public StoredInt currentHeaderID = new StoredInt(this, 0);

		/// The IP address or domain name of the server to connect to.
		public Stored<String> serverAddress = new Stored<>(String.class, this, "192.168.1.105");

		/// The message port of the server where most communication happens.
		///
		/// Defaults to {@value PMPConnection#DEFAULT_MESSAGE_PORT}.
		public StoredInt serverPort = new StoredInt(this, PMPConnection.DEFAULT_MESSAGE_PORT);

		/// The file port of the HTTP server where track files are transferred.
		///
		/// Defaults to {@value PMPConnection#DEFAULT_FILE_PORT}.
		public StoredInt serverFilePort = new StoredInt(this, PMPConnection.DEFAULT_FILE_PORT);

		/// Last known state of playback, including track, position, filters, repeat and shuffle.
		///
		/// Only used to restore state when initializing. Only updated when storage is saved.
		///
		/// @see Player
		public Stored<PlaybackInfo> playbackInfo = new Stored<>(PlaybackInfo.class, this, null);

		/// Last known state of filters on the server side.
		///
		/// Used to decide whether to override server filters upon connecting or to discard local
		/// changes in favor of remote ones.
		public StoredList<FilterInfo> lastKnownServerFilters = new StoredList<>(FilterInfo.class, this);

		/// Queue containing all received library actions that have not been completely handled.
		///
		/// An action is only removed from this queue when an action has finished being handled.
		public StoredBlockingDeque<Action> actionsToHandle = new StoredBlockingDeque<>(Action.class, this);

		/// The ID of the last action received, which may or may not have been handled.
		///
		/// Used to decide whether to request further actions through a [GetActionsRequest] upon connecting.
		///
		/// @see LoginSuccessResponse#lastActionId
		public StoredInt lastReceivedAction = new StoredInt(this, -1);

		/// Queue containing all local library actions that have not been [ActionResponse.Type#COMPLETED].
		///
		/// The client will attempt to send these actions when connecting.
		public StoredBlockingDeque<Action> actionsToSend = new StoredBlockingDeque<>(Action.class, this);

		private Main() {
			super(NAME);
		}
	}

	/// The client sensitive file. Contains login information that must be saved immediately including the private token.
	public static class Sensitive extends SensitiveStorage {
		private static final String NAME = ".sensitive";

		/// The id of this device. Assigned by the server when connecting for the first time.
		///
		/// @see LoginAsExistingDeviceRequest#deviceId
		/// @see LoginSuccessResponse#deviceId
		public Stored<Integer> deviceID = new Stored<>(Integer.class, this, null);

		/// The private token of this device. It is rotated by the server every time the client connects.
		///
		/// @see LoginAsExistingDeviceRequest#token
		/// @see LoginSuccessResponse#token
		public Stored<String> token = new Stored<>(String.class, this, null);

		/// The public encryption key of the server. Stored when connecting to the first time.
		///
		/// Used to ensure the client is connecting to the server it expects.
		public StoredKey serverPublicKey = new StoredKey(this);

		private Sensitive() {
			super(NAME);
		}
	}

	// playing is not here because it should never be playing at startup
	// This record failed to serialize on android. Don't know why, but these have to stay.
	/// Last known state of playback, including track, position, filters, repeat and shuffle.
	public record PlaybackInfo(@JsonProperty("track") String track, @JsonProperty("position") long position,
			@JsonProperty("positiveFilterOptions") List<Pair<Integer, String>> positiveFilterOptions,
			@JsonProperty("negativeFilterOptions") List<Pair<Integer, String>> negativeFilterOptions,
			@JsonProperty("repeat") RepeatOption repeat, @JsonProperty("shuffle") ShuffleOption shuffle) {}
}
