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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Sensitive storage contains particularly important information that should not be shared and must be saved as soon as
 * possible. The difference with {@link Storage} is that sensitive storage immediately stores all changes, while normal
 * storage marks dirty at every change and periodically saves
 */
public class SensitiveStorage extends Storage {
	private static final Logger LOGGER = LogManager.getLogger(SensitiveStorage.class);

	public SensitiveStorage(String name) {
		super(name);
	}

	@Override
	public synchronized void markDirty() {
		if(!NO_DIRTY.orElse(false)) {
			LOGGER.debug("Saving sensitive storage {}", name, new Throwable());
			try {
				save();
			} catch(IOException e) {
				LOGGER.error("Failed to save sensitive storage {} upon change", name, e);

				// maybeSave is still called periodically on sensitive storages. Hope that when it gets called it does
				// not
				// fail again (even though it's really likely that it will)
				dirty = true;
			}
		}
	}
}
