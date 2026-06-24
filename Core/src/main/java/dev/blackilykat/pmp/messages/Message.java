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

package dev.blackilykat.pmp.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/// Base class of all messages.
///
/// Messages are registered by editing this class' annotations.
///
/// All fields, getters and setters without the [JsonIgnore] annotation will be serialized and deserialized
/// when sending and receiving messages.
///
/// Other than particular cases like [Request] and [Response], hierarchy should not be used to group similar
/// messages together due to the lack of actual implementation use cases for doing so.
///
/// Messages are documented with a "direction", containing one or more of the following:
///  - C2S: Client to Server, this message is sent by the client and handled by the server.
///  - S2C: Server to Client, this message is sent by the server and handled by the client.
///  - C2S2C: Client to Server to Client, this message is sent by a client to be forwarded, unaltered or altered,
///    to another client. If the server does anything more than alter and forward the message (such as altering
///    internal state) it must also be marked as C2S.

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "messageType")
@JsonSubTypes({@Type(value = LoginAsNewDeviceRequest.class, name = LoginAsNewDeviceRequest.MESSAGE_TYPE),
		@Type(value = LoginAsExistingDeviceRequest.class, name = LoginAsExistingDeviceRequest.MESSAGE_TYPE),
		@Type(value = LoginSuccessResponse.class, name = LoginSuccessResponse.MESSAGE_TYPE),
		@Type(value = LoginFailResponse.class, name = LoginFailResponse.MESSAGE_TYPE),
		@Type(value = ErrorMessage.class, name = ErrorMessage.MESSAGE_TYPE),
		@Type(value = PlaybackControlMessage.class, name = PlaybackControlMessage.MESSAGE_TYPE),
		@Type(value = PlaybackUpdateMessage.class, name = PlaybackUpdateMessage.MESSAGE_TYPE),
		@Type(value = PlaybackOwnershipMessage.class, name = PlaybackOwnershipMessage.MESSAGE_TYPE),
		@Type(value = FilterListMessage.class, name = FilterListMessage.MESSAGE_TYPE),
		@Type(value = ActionMessage.class, name = ActionMessage.MESSAGE_TYPE),
		@Type(value = ActionRequest.class, name = ActionRequest.MESSAGE_TYPE),
		@Type(value = ActionResponse.class, name = ActionResponse.MESSAGE_TYPE),
		@Type(value = GetActionsRequest.class, name = GetActionsRequest.MESSAGE_TYPE),
		@Type(value = GetActionsResponse.class, name = GetActionsResponse.MESSAGE_TYPE),
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

	/// @return a version of this message, but with all sensitive information redacted. This may or may not be the same
	/// object, but may never mutate the original object.
	public Message withRedactedInfo() {
		return this;
	}
}
