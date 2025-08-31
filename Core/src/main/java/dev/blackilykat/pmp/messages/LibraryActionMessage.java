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
import dev.blackilykat.pmp.LibraryAction;
import dev.blackilykat.pmp.Storage;
import dev.blackilykat.pmp.util.Pair;

import java.util.List;

/**
 * Used to notify of changes in the library. For ADD and REPLACE, the server should wait about 10 seconds for a
 * connection to be made to the http server so the clients can upload their files. Clients can expect a successful
 * transfer if the http server sends back a 200. When clients get an ADD or REPLACE message from the server they can
 * rely on the http server to get the file as well.
 */
public class LibraryActionMessage extends Message {
	public static final String MESSAGE_TYPE = "LibraryAction";

	public Integer actionId;
	public LibraryAction.Type actionType;
	public String fileName;
	public List<Pair<String, String>> newMetadata;

	@JsonCreator
	private LibraryActionMessage() {
	}

	public LibraryActionMessage(LibraryAction.Type type, String fileName) {
		this(type, Storage.getStorage().getAndIncrementCurrentActionId(), fileName);
	}

	public LibraryActionMessage(LibraryAction.Type type, int actionId, String fileName) {
		if(type == LibraryAction.Type.CHANGE_METADATA) {
			throw new IllegalArgumentException(
					"Incorrect constructor: expected List<Pair<String, String>> argument for action type " + type);
		}
		this.actionType = type;
		this.actionId = actionId;
		this.fileName = fileName;
	}

	public LibraryActionMessage(LibraryAction.Type type, String fileName, List<Pair<String, String>> newMetadata) {
		this(type, Storage.getStorage().getAndIncrementCurrentActionId(), fileName, newMetadata);
	}

	public LibraryActionMessage(LibraryAction.Type type, int actionId, String fileName,
			List<Pair<String, String>> newMetadata) {
		if(type != LibraryAction.Type.CHANGE_METADATA) {
			throw new IllegalArgumentException(
					"Incorrect constructor: unexpected List<Pair<String, String>> argument for action type " + type);
		}
		this.actionType = type;
		this.actionId = actionId;
		this.fileName = fileName;
		this.newMetadata = newMetadata;
	}
}
