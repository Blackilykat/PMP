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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "messageType")
@JsonSubTypes({@Type(value = DisconnectMessage.class, name = DisconnectMessage.MESSAGE_TYPE),
		@Type(value = ErrorMessage.class, name = ErrorMessage.MESSAGE_TYPE),
		@Type(value = HeaderListMessage.class, name = HeaderListMessage.MESSAGE_TYPE),
		@Type(value = KeepaliveMessage.class, name = KeepaliveMessage.MESSAGE_TYPE),
		@Type(value = LatestHeaderIdMessage.class, name = LatestHeaderIdMessage.MESSAGE_TYPE),
		@Type(value = LibraryActionMessage.class, name = LibraryActionMessage.MESSAGE_TYPE),
		@Type(value = LibraryActionRequestMessage.class, name = LibraryActionRequestMessage.MESSAGE_TYPE),
		@Type(value = LibraryHashesMessage.class, name = LibraryHashesMessage.MESSAGE_TYPE),
		@Type(value = LoginMessage.class, name = LoginMessage.MESSAGE_TYPE),
		@Type(value = PlaybackSessionCreateMessage.class, name = PlaybackSessionCreateMessage.MESSAGE_TYPE),
		@Type(value = PlaybackSessionDeleteMessage.class, name = PlaybackSessionDeleteMessage.MESSAGE_TYPE),
		@Type(value = PlaybackSessionListMessage.class, name = PlaybackSessionListMessage.MESSAGE_TYPE),
		@Type(value = PlaybackSessionUpdateMessage.class, name = PlaybackSessionUpdateMessage.MESSAGE_TYPE),
		@Type(value = WelcomeMessage.class, name = WelcomeMessage.MESSAGE_TYPE),})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Message implements Cloneable {
	/**
	 * An increasing identifier which should be unique per session (as in every connection will have its own counter)
	 * and per side.
	 */
	public Integer messageId = null;

	/**
	 * Returns a message identical to this one but with a new {@link #messageId}. Does not change the value on this
	 * object.
	 *
	 * @param messageId the new messageId value
	 *
	 * @return A copy of the object with the updated message id
	 */
	public Message withMessageId(Integer messageId) {
		Message copy = this.clone();
		copy.messageId = messageId;
		return copy;
	}

	/**
	 * Creates an identical copy of this message (without the {@link #messageId})
	 */
	@Override
	public Message clone() {
		try {
			Message clone = (Message) super.clone();
			clone.messageId = null;
			return clone;
		} catch(CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
