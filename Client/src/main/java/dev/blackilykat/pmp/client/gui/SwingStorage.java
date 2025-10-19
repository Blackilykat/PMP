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

package dev.blackilykat.pmp.client.gui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.blackilykat.pmp.Storage;
import dev.blackilykat.pmp.client.Main;
import dev.blackilykat.pmp.event.EventSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class SwingStorage {
	public static final EventSource<SwingStorage> EVENT_SAVING = new EventSource<>();

	private static final File FILE = new File("swing.json");
	private static final Logger LOGGER = LogManager.getLogger(SwingStorage.class);

	private static final long SAVING_INTERVAL_MS = 30 * 60 * 1000;

	private static SwingStorage instance = null;
	private static ObjectMapper mapper;

	@JsonIgnore
	private boolean dirty = true;

	private Map<Integer, Integer> headerWidths = new HashMap<>();
	private boolean confirmRemoveTrackPopup = true;

	private SwingStorage() {
		Timer savingTimer = new Timer("Swing storage saving timer");
		savingTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					maybeSave();
				} catch(IOException e) {
					LOGGER.error("Failed to save swing storage", e);
				}
			}
		}, SAVING_INTERVAL_MS, SAVING_INTERVAL_MS);
	}

	public Map<Integer, Integer> getHeaderWidths() {
		return headerWidths;
	}

	public void setHeaderWidths(Map<Integer, Integer> headerWidths) {
		dirty = true;
		this.headerWidths = headerWidths;
	}

	public boolean getConfirmRemoveTrackPopup() {
		return confirmRemoveTrackPopup;
	}

	public void setConfirmRemoveTrackPopup(boolean confirmRemoveTrackPopup) {
		dirty = true;
		this.confirmRemoveTrackPopup = confirmRemoveTrackPopup;
	}

	/**
	 * Saves storage to disk.
	 *
	 * @throws IOException if there is an error writing to storage
	 * @throws IllegalStateException if storage is null or not an instance of {@link SwingStorage}
	 */
	private synchronized void save() throws IOException {
		LOGGER.info("Saving client storage");

		if(FILE.isDirectory()) {
			throw new IOException("storage.json is a directory");
		}

		Random random = new Random();

		File tmpFile;

		do {
			tmpFile = new File(".storage." + random.nextLong() + ".json");
		} while(tmpFile.exists());

		EVENT_SAVING.call(this);

		mapper.writeValue(tmpFile, this);

		Files.move(tmpFile.toPath(), FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);

		dirty = false;
	}

	/**
	 * Loads storage from disk and calls {@link Storage#setStorage}. If storage was never saved, it creates one with
	 * default values.
	 *
	 * @throws IOException if there is an error reading from storage
	 */
	public static void load() throws IOException {
		LOGGER.info("Loading swing storage");
		if(!FILE.exists()) {
			instance = new SwingStorage();
		} else {
			if(FILE.isDirectory()) {
				throw new IOException("swing.json is a directory");
			}
			try {
				instance = mapper.readValue(FILE, SwingStorage.class);
			} catch(IOException e) {
				Random random = new Random();

				try {
					File debugFile;

					do {
						debugFile = new File("debug_swing." + random.nextLong() + ".json");
					} while(debugFile.exists());

					Files.copy(FILE.toPath(), debugFile.toPath());

					LOGGER.error("Failed to load swing storage, defaulting to empty one. Bad file found at {}",
							debugFile.getAbsolutePath(), e);
				} catch(IOException ex) {
					LOGGER.error(
							"Failed to load swing storage, defaulting to empty one. Bad file could not be " +
									"stored.",
							e);
				}

				instance = new SwingStorage();
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
	 * @throws IllegalStateException if storage is null
	 */
	public static void maybeSave() throws IOException {
		SwingStorage clientStorage = getInstance();
		if(clientStorage.dirty) {
			clientStorage.save();
		}
	}

	/**
	 * @throws IllegalStateException if storage is null
	 */
	public static SwingStorage getInstance() {
		if(instance == null) {
			throw new IllegalStateException("Storage was never loaded");
		}
		return instance;
	}

	static {
		mapper = new ObjectMapper();
		mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
	}
}
