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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.Storage;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ServerStorage extends Storage {
	public static final EventSource<ServerStorage> EVENT_SAVING = new EventSource<>();
	/**
	 * Event called when periodically saving to check whether storage is dirty.
	 * <p>Do not use this event to store data. This is not guaranteed to be called at every save. Use
	 * {@link #EVENT_SAVING} for that.
	 * <p>Use {@link MaybeSavingEvent#markDirty()} and return to indicate that there is data to save.
	 */
	public static final EventSource<MaybeSavingEvent> EVENT_MAYBE_SAVING = new EventSource<>();

	private static final File file = new File("storage.json");
	private static final Logger LOGGER = LogManager.getLogger(ServerStorage.class);

	private static final long SAVING_INTERVAL_MS = 30 * 60 * 1000;

	private static ObjectMapper mapper;

	@JsonIgnore
	private boolean dirty = true;

	private int currentFilterID = 0;
	private int currentSessionID = 1;
	private int currentDeviceID = 0;
	private List<Device> devices = List.of();
	private String password = null;

	private String track = null;
	private List<Pair<Integer, String>> positiveFilterOptions = List.of();
	private List<Pair<Integer, String>> negativeFilterOptions = List.of();
	private RepeatOption repeat = RepeatOption.ALL;
	private ShuffleOption shuffle = ShuffleOption.OFF;
	private long position = 0;
	private List<FilterInfo> filters = List.of();
	private List<Track> tracks = List.of();
	// index = action ID
	private List<Action> actions = new LinkedList<>();


	private ServerStorage() {
		Timer savingTimer = new Timer("Server storage saving timer");
		savingTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					maybeSave();
				} catch(IOException e) {
					LOGGER.error("Failed to save storage", e);
				}
			}
		}, SAVING_INTERVAL_MS, SAVING_INTERVAL_MS);
	}

	@Override
	@JsonIgnore
	public int getAndIncrementCurrentFilterId() {
		return super.getAndIncrementCurrentFilterId();
	}

	@Override
	public synchronized int getCurrentFilterID() {
		return currentFilterID;
	}

	@Override
	public synchronized void setCurrentFilterID(int id) {
		dirty = true;
		currentFilterID = id;
	}

	@Override
	@JsonIgnore
	public int getAndIncrementCurrentSessionId() {
		return super.getAndIncrementCurrentSessionId();
	}

	@Override
	public synchronized int getCurrentSessionID() {
		return currentSessionID;
	}

	@Override
	public synchronized void setCurrentSessionID(int id) {
		dirty = true;
		currentSessionID = id;
	}

	@JsonIgnore
	public int getAndIncrementCurrentDeviceId() {
		int id = getCurrentDeviceID();
		setCurrentDeviceID(id + 1);
		return id;
	}

	public synchronized int getCurrentDeviceID() {
		return currentDeviceID;
	}

	public synchronized void setCurrentDeviceID(int id) {
		dirty = true;
		currentDeviceID = id;
	}

	public synchronized List<Device> getDevices() {
		return Collections.unmodifiableList(devices);
	}

	public synchronized void setDevices(List<Device> devices) {
		dirty = true;
		this.devices = new LinkedList<>(devices);
	}

	public synchronized String getPassword() {
		return password;
	}

	public synchronized void setPassword(String password) {
		dirty = true;
		this.password = password;
	}

	public synchronized String getTrack() {
		return track;
	}

	public synchronized void setTrack(String track) {
		dirty = true;
		this.track = track;
	}

	public synchronized ShuffleOption getShuffle() {
		return shuffle;
	}

	public synchronized void setShuffle(ShuffleOption shuffle) {
		dirty = true;
		this.shuffle = shuffle;
	}

	public synchronized RepeatOption getRepeat() {
		return repeat;
	}

	public synchronized void setRepeat(RepeatOption repeat) {
		dirty = true;
		this.repeat = repeat;
	}

	public synchronized List<Pair<Integer, String>> getPositiveFilterOptions() {
		return Collections.unmodifiableList(positiveFilterOptions);
	}

	public synchronized void setPositiveFilterOptions(List<Pair<Integer, String>> positiveFilterOptions) {
		dirty = true;
		this.positiveFilterOptions = new LinkedList<>(positiveFilterOptions);
	}

	public synchronized List<Pair<Integer, String>> getNegativeFilterOptions() {
		return Collections.unmodifiableList(negativeFilterOptions);
	}

	public synchronized void setNegativeFilterOptions(List<Pair<Integer, String>> negativeFilterOptions) {
		dirty = true;
		this.negativeFilterOptions = new LinkedList<>(negativeFilterOptions);
	}

	public synchronized List<FilterInfo> getFilters() {
		return Collections.unmodifiableList(filters);
	}

	public synchronized void setFilters(List<FilterInfo> filters) {
		dirty = true;
		this.filters = new LinkedList<>(filters);
	}

	public synchronized List<Track> getTracks() {
		return Collections.unmodifiableList(tracks);
	}

	public synchronized void setTracks(List<Track> tracks) {
		dirty = true;
		this.tracks = new LinkedList<>(tracks);
	}

	public synchronized List<Action> getActions() {
		return Collections.unmodifiableList(actions);
	}

	public synchronized void setActions(List<Action> actions) {
		dirty = true;
		this.actions = new LinkedList<>(actions);
	}

	public synchronized int nextActionId() {
		return this.actions.size();
	}

	public synchronized void addAction(Action action) {
		dirty = true;
		this.actions.add(action);
	}

	public synchronized long getPosition() {
		return position;
	}

	public synchronized void setPosition(long position) {
		dirty = true;
		this.position = position;
	}

	/**
	 * Saves storage to disk.
	 *
	 * @throws IOException if there is an error writing to storage
	 * @throws IllegalStateException if storage is null or not an instance of {@link ServerStorage}
	 */
	private synchronized void save() throws IOException {
		LOGGER.info("Saving server storage");

		if(file.isDirectory()) {
			throw new IOException("storage.json is a directory");
		}

		Random random = new Random();

		File tmpFile;

		do {
			tmpFile = new File(".storage." + random.nextLong() + ".json");
		} while(tmpFile.exists());

		EVENT_SAVING.call(this);

		mapper.writeValue(tmpFile, this);

		Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

		dirty = false;
	}

	/**
	 * Loads storage from disk and calls {@link Storage#setStorage}. If storage was never saved, it creates one with
	 * default values.
	 *
	 * @throws IOException if there is an error reading from storage
	 */
	public static void load() throws IOException {
		LOGGER.info("Loading server storage");
		if(!file.exists()) {
			Storage.setStorage(new ServerStorage());
		} else {
			if(file.isDirectory()) {
				throw new IOException("storage.json is a directory");
			}
			try {
				Storage.setStorage(mapper.readValue(file, ServerStorage.class));
			} catch(IOException e) {
				Random random = new Random();


				try {
					File debugFile;

					do {
						debugFile = new File("debug_storage." + random.nextLong() + ".json");
					} while(debugFile.exists());

					Files.copy(file.toPath(), debugFile.toPath());

					LOGGER.error("Failed to load storage, defaulting to empty one. Bad file found at {}",
							debugFile.getAbsolutePath(), e);
				} catch(IOException ex) {
					LOGGER.error("Failed to load storage, defaulting to empty one. Bad file could not be stored.", e);
				}

				Storage.setStorage(new ServerStorage());
			}
		}

		Main.EVENT_SHUTDOWN.register(_ -> {
			try {
				maybeSave();
			} catch(IOException e) {
				LOGGER.error("Failed to save storage", e);
			}
		});
	}

	/**
	 * Saves storage to disk if there is new data to save.
	 *
	 * @throws IOException if there is an error writing to storage
	 * @throws IllegalStateException if storage is null or not an instance of {@link ServerStorage}
	 */
	public static void maybeSave() throws IOException {
		ServerStorage serverStorage = getInstance();

		if(serverStorage.dirty) {
			serverStorage.save();
		} else {
			MaybeSavingEvent event = new MaybeSavingEvent(serverStorage);
			EVENT_MAYBE_SAVING.call(event);
			if(event.getDirty()) {
				serverStorage.save();
			}
		}
	}

	/**
	 * Saves storage to disk, without checking whether there's new data to save
	 *
	 * @throws IOException if there is an error writing to storage
	 * @throws IllegalStateException if storage is null or not an instance of {@link ServerStorage}
	 */
	public static void doSave() throws IOException {
		ServerStorage serverStorage = ServerStorage.getInstance();
		serverStorage.save();
	}

	/**
	 * Uses {@link Storage#getStorage()} to get the server storage.
	 *
	 * @throws IllegalStateException if the storage is null or not a ServerStorage
	 */
	public static ServerStorage getInstance() {
		Storage storage = Storage.getStorage();
		if(!(storage instanceof ServerStorage serverStorage)) {
			throw new IllegalStateException("Storage is null or not server storage");
		}
		return serverStorage;
	}

	public static class MaybeSavingEvent {
		public final ServerStorage serverStorage;
		private boolean dirty = false;

		public MaybeSavingEvent(ServerStorage serverStorage) {
			this.serverStorage = serverStorage;
		}

		public void markDirty() {
			dirty = true;

			try {
				LOGGER.debug("Storage marked dirty at {}", new Throwable().getStackTrace()[1]);
			} catch(IndexOutOfBoundsException e) {
				LOGGER.debug("Storage marked dirty at unknown location");
			}
		}

		public boolean getDirty() {
			return dirty;
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

	static {
		mapper = new ObjectMapper();
		mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
	}
}
