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

package dev.blackilykat.pmp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.blackilykat.pmp.util.Pair;

import java.io.Serializable;
import java.util.List;

/// Represents any update to the library.
///
/// Used to maintain a trusted unique order of modifications which stays consistent on all devices.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Action implements Serializable {
	/// The track affected by this action
	public String filename;

	/// What kind of modification this action performs on the library
	public Type actionType;

	/// Only present when [#actionType] == [Type#CHANGE_METADATA]. Contains the full updated metadata of the track.
	public List<Pair<String, String>> newMetadata = null;

	/// Constructor used when actionType != [Type#CHANGE_METADATA].
	public Action(String filename, Type actionType) {
		if(actionType == Type.CHANGE_METADATA) {
			throw new IllegalArgumentException("Wrong initializer for action type CHANGE_METADATA!");
		}
		this.filename = filename;
		this.actionType = actionType;
	}

	/// Constructor used when actionType == [Type#CHANGE_METADATA].
	public Action(String filename, List<Pair<String, String>> newMetadata) {
		this.filename = filename;
		this.actionType = Type.CHANGE_METADATA;
		this.newMetadata = newMetadata;
	}

	/// Constructor used when deserializing actions from JSON.
	@JsonCreator
	private Action() {
		this.filename = "";
	}

	@Override
	public String toString() {
		return "Action{" + "actionType=" + actionType + ", filename='" + filename + '\'' + ", newMetadata="
				+ newMetadata + '}';
	}

	/// What kind of modification an action performs on the library
	public enum Type {
		/// Add a new track to the library
		ADD,
		/// Remove a track from the library
		REMOVE,
		/// Replace the file of a track with another one (would be the same song, this action would only happen if like
		/// someone changes the source, say, to get a higher quality version)
		REPLACE,
		/// Change the metadata of a track while keeping the audio data untouched
		CHANGE_METADATA
	}
}
