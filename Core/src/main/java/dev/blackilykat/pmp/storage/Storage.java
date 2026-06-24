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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.Globals;
import dev.blackilykat.pmp.util.ScopedValue;
import dev.blackilykat.pmp.util.Shutdown;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/// Storages are used to store data to disk to keep it throughout restarts of the program.
///
/// A storage should contain final [Stored] fields. These will be found through reflection and read
/// and written to files.
///
/// Data is serialized with JSON. This allows simple management of storage in code and gives users
/// the possibility of manually editing storage files.
///
/// In most cases the amount of data to store is small. If this is not the case, a field should be
/// extracted to its own storage file to reduce the load of serializing the file it would've
/// otherwise been in.
///
/// Most storages are marked dirty when a value is updated (this happens automatically in [Stored]),
/// to be then saved either periodically every {@value #SAVING_INTERVAL_MIN} minutes or upon
/// [Shutdown#EVENT_MAY_SHUTDOWN_SOON].
///
/// If possible, you should directly use and set values in storage throughout the code, instead of
/// manually creating a separate "loading" and "saving" stage. In some cases, however, it is convenient
/// to separately keep track of state. In those cases you may use [#eventMaybeSaving] to check whether
/// the separately stored state has changed since the last write, and [#eventSaving] to actually set
/// the new values in storage before it gets written to disk.
public abstract class Storage {
	/// Used while loading to prevent the setters of [Stored] fields called while deserializing JSON
	/// from marking the loading storage as dirty.
	public static final ScopedValue<Boolean> NO_DIRTY = ScopedValue.newInstance();

	/// How often storages should periodically save if dirty.
	private static final int SAVING_INTERVAL_MIN = 30;
	private static final long SAVING_INTERVAL_MS = SAVING_INTERVAL_MIN * 60 * 1000;
	private static final Logger LOGGER = LogManager.getLogger(Storage.class);
	private static final ObjectMapper MAPPER;
	private static Timer savingTimer = null;

	/// Emitted before writing a storage to disk after confirming it is dirty.
	///
	/// Use this to write state stored separately to storage.
	public final EventSource<Storage> eventSaving = new EventSource<>();

	/// Emitted before discarding a storage as non-dirty to check if separately
	/// stored state has changed.
	///
	/// Use this exclusively to perform this check and mark storage as dirty if so.
	///
	/// **DO NOT** rely on this to save state to storage. Use [#eventSaving] instead.
	/// In most cases, this will not be called at all.
	///
	/// Avoid performing computationally expensive operations here. If you have
	/// large amounts of data you can usually store it directly in the storage
	/// using a [StoredList] or a [StoredMap]. If you really can't store it directly,
	/// consider whether it is acceptable to lose an update. Most of the time,
	/// other values will make the data get updated regardless, and it may be
	/// worth it to skip the dirty checks for some values.
	public final EventSource<Storage> eventMaybeSaving = new EventSource<>();

	/// The name of the storage, used for logging and to determine the filename.
	@JsonIgnore
	public final String name;

	/// Whether the storage is dirty or not. May be updated last minute when periodically
	/// saving by [#eventMaybeSaving].
	@JsonIgnore
	protected boolean dirty = false;

	public Storage(String name) {
		this.name = name;

		Shutdown.EVENT_MAY_SHUTDOWN_SOON.register(v -> {
			try {
				if(dirty) {
					save();
				}
			} catch(IOException e) {
				LOGGER.error("Failed to save storage", e);
			}
		});
	}

	/// Periodically called to save storage **if dirty**.
	public synchronized void maybeSave() throws IOException {
		if(!dirty) {
			eventMaybeSaving.call(this);
		}
		if(dirty) {
			save();
		}
	}

	/// Saves storage to disk.
	///
	/// @throws IOException if there is an error writing to storage
	protected synchronized void save() throws IOException {
		LOGGER.info("Saving {} storage", this.getClass().getName());

		File file = new File(Globals.dataRoot, name + ".json");

		if(file.isDirectory()) {
			throw new IOException(name + ".json is a directory");
		}

		Random random = new Random();

		File tmpFile;

		// cannot use File#createTempFile because that might put it in ram and defeat the purpose of a temp file
		do {
			tmpFile = new File(Globals.dataRoot, "." + name + "." + random.nextLong() + ".json");
		} while(tmpFile.exists());

		eventSaving.call(this);

		MAPPER.writeValue(tmpFile, this);

		Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		if(name.startsWith(".")) {
			try {
				Files.setAttribute(file.toPath(), "dos:hidden", true);
			} catch(UnsupportedOperationException _) {
			}
		}

		dirty = false;
	}

	/// Marks this storage as dirty, to be saved periodically or during shutdown.
	///
	/// This method logs a stacktrace if storage was not dirty. This is not a warning,
	/// but an aid to debugging potential optimization issues.
	public synchronized void markDirty() {
		if(!dirty && !NO_DIRTY.orElse(false)) {
			LOGGER.debug("Storage {} marked dirty", name, new Throwable());
			dirty = true;
		}
	}

	/// Load a storage from disk.
	///
	/// @param name the storage name used to determine the filename.
	/// @param clazz the class of the storage to load.
	public static <T extends Storage> T load(String name, Class<T> clazz) throws IOException {
		LOGGER.info("Loading {} storage", name);

		File file = new File(Globals.dataRoot, name + ".json");

		LOGGER.debug("Attempting to load {} storage at {}", name, file.getAbsolutePath());

		AtomicReference<T> toReturn = new AtomicReference<>(null);
		AtomicReference<IOException> exe = new AtomicReference<>(null);
		ScopedValue.where(NO_DIRTY, true).run(() -> {
			try {
				Constructor<T> constructor = clazz.getDeclaredConstructor();
				constructor.setAccessible(true);

				if(!file.exists()) {
					toReturn.set(constructor.newInstance());
				} else {
					if(file.isDirectory()) {
						exe.set(new IOException(name + ".json is a directory"));
						return;
					}
					try {
						toReturn.set(constructor.newInstance());
						ObjectNode node = MAPPER.readValue(file, ObjectNode.class);
						loadValues(toReturn.get(), node, clazz);
					} catch(IOException e) {
						Random random = new Random();

						try {
							File debugFile;

							do {
								debugFile = new File(".debug_" + name + "." + random.nextLong() + ".json");
							} while(debugFile.exists());

							Files.copy(file.toPath(), debugFile.toPath());

							LOGGER.error("Failed to load storage, defaulting to empty one. Bad file found at {}",
									debugFile.getAbsolutePath(), e);
						} catch(IOException ex) {
							LOGGER.error(
									"Failed to load storage, defaulting to empty one. Bad file could not be stored.",
									e);
						}

						toReturn.set(constructor.newInstance());
					}
				}
			} catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException |
			        InstantiationException e) {
				LOGGER.error("Tried to load a bad storage class", e);
				throw new IllegalArgumentException("Bad storage class");
			}

			if(savingTimer == null) {
				savingTimer = new Timer("Storage saving timer");
			}

			final T ftr = toReturn.get();
			savingTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						ftr.maybeSave();
					} catch(IOException e) {
						LOGGER.error("Failed to save storage", e);
					}
				}
			}, SAVING_INTERVAL_MS, SAVING_INTERVAL_MS);
		});
		if(exe.get() != null) {
			throw exe.get();
		}
		return toReturn.get();
	}

	private static <T extends Storage> void loadValues(T storage, ObjectNode json, Class<T> clazz)
			throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

		// needed to work around generics
		Method setter = Stored.class.getDeclaredMethod("set", Object.class);

		for(Field field : clazz.getDeclaredFields()) {
			if(!Stored.class.isAssignableFrom(field.getType())) {
				continue;
			}
			JsonNode jsonValue = json.get(field.getName());
			if(jsonValue == null || jsonValue.isNull()) {
				continue;
			}

			Type valueType = ((Stored<?>) field.get(storage)).type;

			Object newValue = MAPPER.treeToValue(jsonValue, TypeFactory.defaultInstance().constructType(valueType));
			setter.invoke(field.get(storage), newValue);
		}
	}

	static {
		MAPPER = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(new Stored.Serializer());
		MAPPER.registerModule(module);
		MAPPER.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
	}
}
