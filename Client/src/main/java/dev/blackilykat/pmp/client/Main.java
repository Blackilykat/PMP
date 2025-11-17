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

import dev.blackilykat.pmp.client.gui.MainWindow;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.util.LoggingProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Main {
	public static final Logger LOGGER = LogManager.getLogger(Main.class);
	public static final EventSource<Void> EVENT_SHUTDOWN = new EventSource<>();

	private static boolean shuttingDown = false;

	static void main(String[] args) {
		logDebugSystemInfo();
		LOGGER.info("Starting client");

		LoggingProxy.setUpProxies();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown(false);
		}));

		try {
			ClientStorage.load();
		} catch(IOException e) {
			LOGGER.fatal("Failed to load storage, exiting", e);
			System.exit(1);
		}

		new Thread(() -> {
			Library.init();

			Player.init();
		}).start();
		MainWindow.main(args);

		Server.connect();
	}

	public static void shutdown(boolean exit) {
		if(shuttingDown) {
			return;
		}
		shuttingDown = true;

		EVENT_SHUTDOWN.call(null);

		LOGGER.info("Shutting down");

		if(exit) {
			System.exit(0);
		}
	}

	private static void logDebugSystemInfo() {
		LOGGER.debug("java.vm.vendor: {}", System.getProperty("java.vm.vendor"));
		LOGGER.debug("java.vendor.url: {}", System.getProperty("java.vendor.url"));
		LOGGER.debug("jdk.debug: {}", System.getProperty("jdk.debug"));
		LOGGER.debug("java.version.date: {}", System.getProperty("java.version.date"));
		LOGGER.debug("java.runtime.version: {}", System.getProperty("java.runtime.version"));
		LOGGER.debug("java.vendor.version: {}", System.getProperty("java.vendor.version"));
		LOGGER.debug("java.vm.version: {}", System.getProperty("java.vm.version"));
		LOGGER.debug("java.vm.name: {}", System.getProperty("java.vm.name"));
		LOGGER.debug("java.vendor.url.bug: {}", System.getProperty("java.vendor.url.bug"));
		LOGGER.debug("java.class.version: {}", System.getProperty("java.class.version"));
		LOGGER.debug("awt.toolkit.name: {}", System.getProperty("awt.toolkit.name"));
		LOGGER.debug("os.name: {}", System.getProperty("os.name"));
		LOGGER.debug("os.version: {}", System.getProperty("os.version"));
		LOGGER.debug("os.arch: {}", System.getProperty("os.arch"));
	}
}
