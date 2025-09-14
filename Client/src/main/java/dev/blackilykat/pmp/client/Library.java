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

import dev.blackilykat.pmp.event.RetroactiveEventSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Library {
	public static final RetroactiveEventSource<List<Track>> EVENT_LOADED = new RetroactiveEventSource<>();
	private static final Logger LOGGER = LogManager.getLogger(Library.class);
	private static File library = null;
	private static boolean initialized = false;
	private static List<Track> tracks = new LinkedList<>();

	public static List<Track> getAllTracks() {
		maybeInit();
		return tracks;
	}

	public static void maybeInit() {
		if(initialized) {
			return;
		}
		LOGGER.info("Initializing library");

		library = new File("library");
		if(!library.exists()) {
			library.mkdirs();
		}
		if(!library.isDirectory()) {
			LOGGER.error("Library is a file");
			throw new IllegalStateException("Library is a file");
		}

		File[] children = library.listFiles();
		if(children != null) {
			for(File file : children) {
				try {
					tracks.add(new Track(file));
				} catch(IOException e) {
					LOGGER.error("Error on track {}", file.getName());
				}
			}
		}

		EVENT_LOADED.call(tracks);

		initialized = true;
		LOGGER.info("Initialized library");
	}
}
