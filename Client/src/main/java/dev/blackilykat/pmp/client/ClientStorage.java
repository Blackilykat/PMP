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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.Storage;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ClientStorage extends Storage {
	public static final EventSource<ClientStorage> EVENT_SAVING = new EventSource<>();
	/**
	 * Event called when periodically saving to check whether storage is dirty.
	 * <p>Do not use this event to store data. This is not guaranteed to be called at every save. Use
	 * {@link #EVENT_SAVING} for that.
	 * <p>Use {@link MaybeSavingEvent#markDirty()} and return to indicate that there is data to save.
	 */
	public static final EventSource<MaybeSavingEvent> EVENT_MAYBE_SAVING = new EventSource<>();

	private static final File file = new File("storage.json");
	private static final Logger LOGGER = LogManager.getLogger(ClientStorage.class);

	private static final long SAVING_INTERVAL_MS = 30 * 60 * 1000;

	private static ObjectMapper mapper;

	@JsonIgnore
	private boolean dirty = true;

	private List<Track> trackCache = new LinkedList<>();
	// null: empty configuration is technically valid, but not default
	private List<Header> headers = null;
	private List<Filter> filters = null;
	private int currentFilterID = 0;
	private int currentSessionID = 0;
	private int currentHeaderID = 0;
	private String serverAddress = "localhost";
	private int serverPort = PMPConnection.DEFAULT_MESSAGE_PORT;
	private int serverFilePort = PMPConnection.DEFAULT_FILE_PORT;
	/**
	 * Base64 encoded serialized {@link Key} object
	 */
	private String encodedServerPublicKey = null;
	private String token = null;
	private Integer deviceID = null;
	private PlaybackInfo playbackInfo;
	private List<FilterInfo> lastKnownServerFilters = List.of();
	private BlockingDeque<Action> actionsToHandle = new LinkedBlockingDeque<>();
	private int lastReceivedAction = -1;
	private BlockingDeque<Action> actionsToSend = new LinkedBlockingDeque<>();

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

	public synchronized List<Header> getHeaders() {
		return headers == null ? null : Collections.unmodifiableList(headers);
	}

	public synchronized void setHeaders(List<Header> headers) {
		dirty = true;
		this.headers = headers;
	}

	public synchronized List<Track> getTrackCache() {
		return Collections.unmodifiableList(trackCache);
	}

	public synchronized void setTrackCache(List<Track> trackCache) {
		dirty = true;
		this.trackCache = new LinkedList<>(trackCache);
	}

	public synchronized PlaybackInfo getPlaybackInfo() {
		return playbackInfo;
	}

	public synchronized void setPlaybackInfo(PlaybackInfo playbackInfo) {
		dirty = true;
		this.playbackInfo = playbackInfo;
	}

	public synchronized List<Filter> getFilters() {
		if(filters == null) {
			return null;
		}
		return Collections.unmodifiableList(filters);
	}

	public synchronized void setFilters(List<Filter> filters) {
		dirty = true;
		this.filters = new LinkedList<>(filters);
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		dirty = true;
		this.serverAddress = serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		dirty = true;
		this.serverPort = serverPort;
	}

	public int getServerFilePort() {
		return serverFilePort;
	}

	public void setServerFilePort(int serverFilePort) {
		dirty = true;
		this.serverFilePort = serverFilePort;
	}

	@JsonIgnore
	public Key getServerPublicKey() {
		if(encodedServerPublicKey == null) {
			return null;
		}

		byte[] data = Base64.getDecoder().decode(encodedServerPublicKey);
		try(ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(data))) {
			Object o = is.readObject();
			if(!(o instanceof Key key)) {
				LOGGER.error("Storage#getServerPublicKey: Key is not a key but a {}, this should be unreachable",
						o.getClass().getName());
				return null;
			}
			return key;
		} catch(IOException | ClassNotFoundException e) {
			LOGGER.error("Storage#getServerPublicKey: this should be unreachable", e);
			return null;
		}
	}

	@JsonIgnore
	public void setServerPublicKey(Key serverPublicKey) {
		dirty = true;
		if(this.encodedServerPublicKey != null) {
			LOGGER.warn("Overriding server public key");
		}
		try(ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(byteStream)) {

			os.writeObject(serverPublicKey);
			byte[] data = byteStream.toByteArray();
			this.encodedServerPublicKey = Base64.getEncoder().encodeToString(data);
		} catch(IOException e) {
			LOGGER.error("Storage#getServerPublicKey: this should be unreachable", e);
		}
	}

	public String getEncodedServerPublicKey() {
		return encodedServerPublicKey;
	}

	private void setEncodedServerPublicKey(String encodedServerPublicKey) {
		this.encodedServerPublicKey = encodedServerPublicKey;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		dirty = true;
		this.token = token;
	}

	public Integer getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(Integer deviceID) {
		dirty = true;
		this.deviceID = deviceID;
	}

	public synchronized List<FilterInfo> getLastKnownServerFilters() {
		return Collections.unmodifiableList(lastKnownServerFilters);
	}

	public synchronized void setLastKnownServerFilters(List<FilterInfo> filters) {
		dirty = true;
		this.lastKnownServerFilters = new LinkedList<>(filters);
	}

	public synchronized @Nullable Action peekActionsToHandle() {
		return actionsToHandle.peek();
	}

	public synchronized void takeActionToHandle() {
		dirty = true;
		actionsToHandle.remove();
	}

	public @Nonnull Action peekActionsToHandleBlocking() throws InterruptedException {
		// cannot replace with peek(), this needs to block

		Action action = actionsToHandle.take();
		actionsToHandle.addFirst(action);
		return action;
	}

	public synchronized void addActionToHandle(Action action) {
		dirty = true;
		actionsToHandle.add(action);
	}

	public synchronized int getLastReceivedAction() {
		return lastReceivedAction;
	}

	public synchronized void setLastReceivedAction(int lastReceivedAction) {
		dirty = true;
		this.lastReceivedAction = lastReceivedAction;
	}

	public synchronized void incrementLastReceivedAction() {
		dirty = true;
		this.lastReceivedAction++;
	}

	public synchronized @Nullable Action peekActionsToSend() {
		return actionsToSend.peek();
	}

	public synchronized void takeActionToSend() {
		dirty = true;
		actionsToSend.remove();
	}

	public @Nonnull Action peekActionsToSendBlocking() throws InterruptedException {
		Action action = actionsToSend.take();
		actionsToSend.addFirst(action);
		return action;
	}

	public synchronized void addActionToSend(Action action) {
		dirty = true;
		actionsToSend.add(action);
	}

	public synchronized Action[] viewAllActionsToSend() {
		return actionsToSend.toArray(new Action[0]);
	}

	public synchronized Action[] viewAllActionsToHandle() {
		return actionsToHandle.toArray(new Action[0]);
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
		} else {
			MaybeSavingEvent event = new MaybeSavingEvent(clientStorage);
			EVENT_MAYBE_SAVING.call(event);
			if(event.getDirty()) {
				clientStorage.save();
			}
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

	public static class MaybeSavingEvent {
		public final ClientStorage clientStorage;
		private boolean dirty = false;

		public MaybeSavingEvent(ClientStorage clientStorage) {
			this.clientStorage = clientStorage;
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

	// playing is not here because it should never be playing at startup
	public record PlaybackInfo(String track, long position, List<Pair<Integer, String>> positiveFilterOptions,
			List<Pair<Integer, String>> negativeFilterOptions, RepeatOption repeat, ShuffleOption shuffle) {}

	static {
		mapper = new ObjectMapper();
		mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
	}
}
