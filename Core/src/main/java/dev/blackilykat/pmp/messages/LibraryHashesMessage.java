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

package dev.blackilykat.pmp.messages;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends every track's filename along with its crc32 hash. Used to make sure libraries don't get de-synced, which would
 * ideally happen only if someone goes out of their way to manually edit music files outside through the application.
 * This would also help discover and fix de-sync caused due to bugs though.
 */
public class LibraryHashesMessage extends Message {
	public static final String MESSAGE_TYPE = "LibraryHashes";

	public Map<String, Long> hashes;

	public LibraryHashesMessage() {
		hashes = new HashMap<>();
	}

	@JsonCreator
	public LibraryHashesMessage(Map<String, Long> hashes) {
		this.hashes = hashes;
	}
}
