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

package dev.blackilykat.pmp;

import dev.blackilykat.pmp.messages.LibraryActionMessage;
import dev.blackilykat.pmp.util.Pair;

import java.io.Serializable;
import java.util.List;

public class LibraryAction implements Serializable {
	public String fileName;

	public Type actionType;

	public List<Pair<String, String>> newMetadata = null;

	public LibraryAction(String fileName, Type actionType) {
		if(actionType == Type.CHANGE_METADATA) {
			throw new IllegalArgumentException("Wrong initializer for action type CHANGE_METADATA!");
		}
		this.fileName = fileName;
		this.actionType = actionType;
	}

	public LibraryAction(String fileName, List<Pair<String, String>> newMetadata) {
		this.fileName = fileName;
		this.actionType = Type.CHANGE_METADATA;
		this.newMetadata = newMetadata;
	}

	public LibraryAction() {
		this.fileName = "";
	}

	public LibraryActionMessage toMessage() {
		if(actionType != Type.CHANGE_METADATA) {
			return new LibraryActionMessage(actionType, fileName);
		} else {
			return new LibraryActionMessage(actionType, fileName, newMetadata);
		}
	}

	public enum Type {
		/**
		 * Add a new song to the library
		 */
		ADD,
		/**
		 * Remove a song from the library
		 */
		REMOVE,
		/**
		 * Replace the file of a song with another one (would be the same song, this action would only happen if like
		 * someone changes the source, say, to get a higher quality version. This action exists so that when the
		 * playback eventually gets tracked the counts don't get split or interrupted due to a file replacement)
		 */
		REPLACE,
		/**
		 * Change the metadata of a song while keeping the audio data untouched
		 */
		CHANGE_METADATA
	}
}
