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
@JsonSubTypes({@Type(value = LoginAsNewDeviceRequest.class, name = LoginAsNewDeviceRequest.MESSAGE_TYPE),
		@Type(value = LoginAsExistingDeviceRequest.class, name = LoginAsExistingDeviceRequest.MESSAGE_TYPE),
		@Type(value = LoginSuccessResponse.class, name = LoginSuccessResponse.MESSAGE_TYPE),
		@Type(value = LoginFailResponse.class, name = LoginFailResponse.MESSAGE_TYPE),
		@Type(value = DisconnectMessage.class, name = DisconnectMessage.MESSAGE_TYPE),})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Message implements Cloneable {

	@Override
	public Message clone() {
		try {
			return (Message) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	/**
	 * @return a version of this message, but with all sensitive information redacted. This may or may not be the same
	 * object, but may never mutate the original object.
	 */
	public Message withRedactedInfo() {
		return this;
	}
}
