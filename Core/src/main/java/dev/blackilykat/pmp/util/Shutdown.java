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

package dev.blackilykat.pmp.util;

import dev.blackilykat.pmp.event.EventSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Utility class to gracefully terminate the execution of the program.
public class Shutdown {
	/// Event called when the program shuts down. This is not guaranteed to be called in every platform (i.e. Android
	/// does not offer a callback on shutdown)
	///
	/// @see #EVENT_MAY_SHUTDOWN_SOON
	public static final EventSource<Void> EVENT_SHUTDOWN = new EventSource<>();
	/// Event called when either the program is shutting down or it may shut down soon. This should be used to save data
	/// to disk, but it should not close any resources that may be needed later. It may be called multiple times during
	/// the execution of the program.
	///
	/// @see #EVENT_SHUTDOWN
	public static final EventSource<Void> EVENT_MAY_SHUTDOWN_SOON = new EventSource<>();
	private static final Logger LOGGER = LogManager.getLogger(Shutdown.class);
	/// True if the shutdown procedure has started.
	///
	/// Prevents making the shutdown procedure which calls shutdown hooks from calling the shutdown procedure again.
	private static boolean shuttingDown = false;

	/// Emit [#EVENT_MAY_SHUTDOWN_SOON].
	public static void mayShutdownSoon() {
		LOGGER.info("May shut down soon");
		EVENT_MAY_SHUTDOWN_SOON.call(null);
	}

	/// The shutdown procedure.
	///
	/// Calls [#EVENT_MAY_SHUTDOWN_SOON] then [#EVENT_SHUTDOWN], then exits if `exit` is true.
	///
	/// @param exit whether this method should actually shut down the JVM.
	public static void shutdown(boolean exit) {
		if(shuttingDown) {
			return;
		}
		shuttingDown = true;

		LOGGER.info("Shutting down");

		EVENT_MAY_SHUTDOWN_SOON.call(null);
		EVENT_SHUTDOWN.call(null);

		if(exit) {
			System.exit(0);
		}
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown(false);
		}));
	}
}
