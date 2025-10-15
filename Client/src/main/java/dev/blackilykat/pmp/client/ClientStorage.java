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

package dev.blackilykat.pmp.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.blackilykat.pmp.Storage;
import dev.blackilykat.pmp.event.EventSource;
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

public class ClientStorage extends Storage {
	public static final EventSource<ClientStorage> EVENT_SAVING = new EventSource<>();

	private static final File file = new File("storage.json");
	private static final Logger LOGGER = LogManager.getLogger(ClientStorage.class);

	private static final long SAVING_INTERVAL_MS = 30 * 60 * 1000;

	private static ObjectMapper mapper;

	@JsonIgnore
	private boolean dirty = true;

	private List<Track> trackCache = new LinkedList<>();
	private int currentActionID = 0;
	private int currentHeaderID = 0;


	private ClientStorage() {
		Timer savingTimer = new Timer("Client storage saving timer");
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
	public int getAndIncrementCurrentActionId() {
		return super.getAndIncrementCurrentActionId();
	}

	@Override
	public synchronized int getCurrentActionID() {
		return currentActionID;
	}

	@Override
	public synchronized void setCurrentActionID(int id) {
		dirty = true;
		currentActionID = id;
	}


	@JsonIgnore
	public int getAndIncrementCurrentHeaderId() {
		int hi = getCurrentHeaderID();
		setCurrentHeaderID(hi + 1);
		return hi;
	}

	public synchronized int getCurrentHeaderID() {
		return currentHeaderID;
	}

	public synchronized void setCurrentHeaderID(int id) {
		dirty = true;
		currentHeaderID = id;
	}

	public synchronized List<Track> getTrackCache() {
		return Collections.unmodifiableList(trackCache);
	}

	public synchronized void setTrackCache(List<Track> trackCache) {
		dirty = true;
		this.trackCache = new LinkedList<>(trackCache);
	}

	/**
	 * Saves storage to disk.
	 *
	 * @throws IOException if there is an error writing to storage
	 * @throws IllegalStateException if storage is null or not an instance of {@link ClientStorage}
	 */
	private synchronized void save() throws IOException {
		LOGGER.info("Saving client storage");

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
		LOGGER.info("Loading client storage");
		if(!file.exists()) {
			Storage.setStorage(new ClientStorage());
		} else {
			if(file.isDirectory()) {
				throw new IOException("storage.json is a directory");
			}
			try {
				Storage.setStorage(mapper.readValue(file, ClientStorage.class));
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

				Storage.setStorage(new ClientStorage());
			}
		}

		Main.EVENT_SHUTDOWN.register(v -> {
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
	 * @throws IllegalStateException if storage is null or not an instance of {@link ClientStorage}
	 */
	public static void maybeSave() throws IOException {
		ClientStorage clientStorage = getInstance();
		if(clientStorage.dirty) {
			clientStorage.save();
		}
	}

	/**
	 * Uses {@link Storage#getStorage()} to get the client storage.
	 *
	 * @throws IllegalStateException if the storage is null or not a ClientStorage
	 */
	public static ClientStorage getInstance() {
		Storage storage = Storage.getStorage();
		if(!(storage instanceof ClientStorage clientStorage)) {
			throw new IllegalStateException("Storage is null or not client storage");
		}
		return clientStorage;
	}

	static {
		mapper = new ObjectMapper();
		mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
	}
}
