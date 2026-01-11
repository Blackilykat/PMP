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

public class Shutdown {
	public static final EventSource<Void> EVENT_SHUTDOWN = new EventSource<>();
	private static final Logger LOGGER = LogManager.getLogger(Shutdown.class);
	private static boolean shuttingDown = false;

	public static void shutdown(boolean exit) {
		if(shuttingDown) {
			return;
		}
		shuttingDown = true;

		LOGGER.info("Shutting down");

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
