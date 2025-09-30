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
import java.util.LinkedList;
import java.util.List;

public class ClientStorage extends Storage {
	public static final EventSource<ClientStorage> EVENT_SAVING = new EventSource<>();

	private static final File file = new File("storage.json");
	private static final Logger LOGGER = LogManager.getLogger(ClientStorage.class);

	private static ObjectMapper mapper;

	public List<Track> trackCache = new LinkedList<>();
	private int currentActionID = 0;


	private ClientStorage() {
	}

	@Override
	@JsonIgnore
	public int getAndIncrementCurrentActionId() {
		return super.getAndIncrementCurrentActionId();
	}

	@Override
	public int getCurrentActionID() {
		return currentActionID;
	}

	@Override
	public void setCurrentActionID(int id) {
		currentActionID = id;
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
			Storage.setStorage(mapper.readValue(file, ClientStorage.class));
		}

		Main.EVENT_SHUTDOWN.register(v -> {
			try {
				save();
			} catch(IOException e) {
				LOGGER.error("Failed to save storage", e);
			}
		});
	}

	/**
	 * Saves storage to disk.
	 *
	 * @throws IOException if there is an error writing to storage
	 * @throws IllegalStateException if storage is null or not an instance of {@link ClientStorage}
	 */
	public static void save() throws IOException {
		LOGGER.info("Saving client storage");
		ClientStorage clientStorage = getInstance();

		if(file.isDirectory()) {
			throw new IOException("storage.json is a directory");
		}

		EVENT_SAVING.call(clientStorage);

		mapper.writeValue(file, clientStorage);
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
