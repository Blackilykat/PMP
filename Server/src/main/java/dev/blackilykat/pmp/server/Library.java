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
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.messages.ActionResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;

/// Server-side library management.
public class Library {
	/// Emitted when an action has been completed successfully.
	public static final EventSource<Void> EVENT_SUCCESSFUL_ACTION = new EventSource<>();
	/// The directory containing the tracks.
	public static final File LIBRARY = new File("library");
	private static final Logger LOGGER = LogManager.getLogger(Library.class);
	/// The library action which has been [ActionResponse.Type#APPROVED] but which the server is still
	/// waiting for the client to [ActionResponse.Type#COMPLETED].
	private static final AtomicReference<PendingAction> PENDING_ACTION = new AtomicReference<>(null);

	/// Initializes the library:
	/// - Creates the [#LIBRARY] directory if it does not exist;
	/// - Checks that cache matches the files in the library and, if not, updates it.
	public static void init() {
		LOGGER.info("Initializing library...");
		if(LIBRARY.exists() && !LIBRARY.isDirectory()) {
			LOGGER.fatal("library is a file, cannot initialize library");
			System.exit(1);
		}
		if(!LIBRARY.exists()) {
			var _ = LIBRARY.mkdirs();
		}

		try {
			File[] filesInLibraryDir = LIBRARY.listFiles();
			assert filesInLibraryDir != null;
			int cachedCount = 0;
			for(File file : filesInLibraryDir) {
				if(!ServerStorage.MAIN.tracks.containsKey(file.getName())) {
					LOGGER.warn("Track {} not cached", file.getName());
					ServerStorage.MAIN.tracks.put(file.getName(), new Track(file));
				} else if(ServerStorage.MAIN.tracks.get(file.getName()).lastModified != file.lastModified()) {
					LOGGER.warn("Track {} had outdated cache", file.getName());
					ServerStorage.MAIN.tracks.put(file.getName(), new Track(file));
				} else {
					cachedCount++;
				}
			}

			LOGGER.info("{} tracks cached", cachedCount);
		} catch(IOException e) {
			LOGGER.fatal("Failed to read library", e);
			System.exit(1);
		}
	}

	/// Adds a track to the library. Overrides any previous track with the same filename.
	///
	/// @param filename the file name for the new track
	/// @param is InputStream with the file contents
	///
	/// @throws IOException if there is an unexpected I/O error while saving the file
	/// @throws IllegalArgumentException if the track is not a valid FLAC file
	public static void add(String filename, InputStream is) throws IOException, IllegalArgumentException {
		File tmpFile = LIBRARY.toPath().resolve(filename + ".tmp").toFile();

		Files.copy(is, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

		try {
			Track tmpTrack = new Track(tmpFile);
			// no exception, valid FLAC format

			File target = LIBRARY.toPath().resolve(filename).toFile();
			try {
				Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			} catch(AtomicMoveNotSupportedException _) {
				boolean success = tmpFile.renameTo(target);
				assert success;
			}

			Track track = new Track(filename, target.lastModified(), tmpTrack.checksum, tmpTrack.metadata);

			ServerStorage.MAIN.tracks.put(filename, track);
		} catch(IOException e) {
			var _ = tmpFile.delete();
			throw new IllegalArgumentException("Not a valid FLAC file");
		}
	}

	/// Remove the track with the given filename from the library.
	///
	/// @throws FileNotFoundException if such track does not exist.
	/// @throws IOException if there was an IO error while deleting the file.
	public static void remove(String filename) throws IOException {
		Track target = ServerStorage.MAIN.tracks.get(filename);

		if(target == null) {
			throw new FileNotFoundException();
		}
		if(!target.file.delete()) {
			throw new IOException("Failed to delete file");
		}

		ServerStorage.MAIN.tracks.remove(filename);
	}

	/// If the pending action can be updated, sets it to the new value
	///
	/// @return whether it was possible to update the pending action
	public static boolean setPendingActionIfPossible(Action action, Device device) {
		synchronized(PENDING_ACTION) {
			if(!isPendingActionOverrideable()) {
				return false;
			}

			PENDING_ACTION.set(new PendingAction(action, device));
			return true;
		}
	}

	/// Returns true if it is possible to update the pending action.
	///
	/// @return true if there is no pending action or the pending action is expired and has not started.
	private static boolean isPendingActionOverrideable() {
		PendingAction action = PENDING_ACTION.get();
		synchronized(PENDING_ACTION) {
			return action == null || (
					System.currentTimeMillis() - action.creationTime > PendingAction.CONNECTION_TIMEOUT_SECONDS * 1000
							&& !action.started);
		}
	}

	/// Checks whether the pending action matches the given details and if it may be started, then marks it as started.
	///
	/// @return Whether the action matched and was marked as started.
	public static boolean startPendingAction(Device device, String filename) {
		synchronized(PENDING_ACTION) {
			PendingAction action = PENDING_ACTION.get();
			if(action == null) {
				return false;
			}
			if(action.device != device) {
				return false;
			}
			if(!action.action.filename.equals(filename)) {
				return false;
			}
			if(System.currentTimeMillis() - action.creationTime > PendingAction.CONNECTION_TIMEOUT_SECONDS * 1000) {
				return false;
			}
			if(action.started) {
				return false;
			}

			action.started = true;
			return true;
		}
	}

	/// Marks the pending action as finished, allowing others to take its place.
	public static void finishPendingAction(boolean successful) {
		synchronized(PENDING_ACTION) {
			PENDING_ACTION.set(null);
			PENDING_ACTION.notify();
		}
		if(successful) {
			EVENT_SUCCESSFUL_ACTION.call(null);
		}
	}

	/// Blocks until it is possible to update the pending action (until [#isPendingActionOverrideable] is true)
	public static void waitForFreePendingAction() throws InterruptedException {
		synchronized(PENDING_ACTION) {
			while(!isPendingActionOverrideable()) {
				// Since having the timeout expire is a rare occurrence, it's fine to have a timeout here.
				// Having a chain of timers or whatnot just to avoid polling adds clutter for very little gain
				PENDING_ACTION.wait((long) (PendingAction.CONNECTION_TIMEOUT_SECONDS * 1000));
			}
		}
	}

	/// A library action which has been [ActionResponse.Type#APPROVED] but which the server is still
	/// waiting for the client to [ActionResponse.Type#COMPLETED].
	public static class PendingAction {
		/// How much time, in seconds, the client has to **start** the HTTP request to complete this
		/// action
		public static final double CONNECTION_TIMEOUT_SECONDS = 30;
		/// When this action was [ActionResponse.Type#APPROVED], and when the [#CONNECTION_TIMEOUT_SECONDS]
		/// started
		public long creationTime;
		/// Which device has started this action
		public Device device;
		/// The not-yet-completed action details
		public Action action;
		/// Whether the HTTP request to complete this action has begun
		public boolean started;

		/// Create a pending action that starts now
		public PendingAction(Action action, Device device) {
			this(action, device, System.currentTimeMillis());
		}

		public PendingAction(Action action, Device device, long creationTime) {
			this.action = action;
			this.device = device;
			this.creationTime = creationTime;
		}
	}
}
