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
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Used to communicate that an expected or unexpected error of any kind has occurred.
 * <p>
 * Only the server is allowed to send errors.
 */
public class ErrorMessage extends Message {
	public static final String MESSAGE_TYPE = "Error";

	/**
	 * Human-readable information about the error (could be a stacktrace/exception name for {@link ErrorType#SERVER})
	 * <p>
	 * If empty, it is not included in the message.
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public String info = "";

	/**
	 * If the error was triggered by a message, the {@link Message#messageId} of said message.
	 */
	public Integer relativeToMessage = null;
	public ErrorType errorType = null;
	public ErrorId errorId = null;

	@JsonCreator
	private ErrorMessage() {
	}

	public ErrorMessage(ErrorType errorType) {
		this.errorType = errorType;
	}

	public ErrorMessage(ErrorId errorId) {
		this.errorId = errorId;
		this.errorType = errorId.type;
	}

	/**
	 * Generally, what caused the error
	 */
	public enum ErrorType {
		/**
		 * An error occurred internally in the server, there is no further information available (but there may be an
		 * explanation in {@link #info})
		 */
		SERVER,
		/**
		 * The client sent an incorrectly formatted message.
		 */
		MESSAGE_FORMAT,
		/**
		 * The client sent a correctly formatted message with non-missing invalid contents.
		 */
		MESSAGE_INVALID_CONTENTS,
		/**
		 * The client sent a correctly formatted message with missing contents.
		 */
		MESSAGE_MISSING_CONTENTS,
		/**
		 * The client tried to something that cannot be done at the moment (for example, adding a file to the library
		 * while another client is already doing that).
		 */
		BUSY
	}


	/**
	 * The specific case of where the error has occurred. This can be used by the client to determine an eventual
	 * recovery from the error (e.g. prompt for password again, reconnect, wait before retrying...) For
	 * retro-compatibility, avoid renaming these if not in major version changes.
	 */
	public enum ErrorId {
		/**
		 * A message the client sent had contents which prove that the client is in an invalid state. The client should
		 * respond by somehow attempting to fix the invalid state (which in most cases would probably mean
		 * reconnecting).
		 */
		INVALID_CLIENT_STATE(ErrorType.MESSAGE_INVALID_CONTENTS),

		/**
		 * The client tried to send a message but hasn't logged in yet.
		 */
		LOGGED_OUT(ErrorType.MESSAGE_INVALID_CONTENTS),

		/**
		 * The client tried to perform a library action while another was already performing one. This shouldn't be a
		 * problem in of itself but to avoid edge cases and race conditions this is currently unsupported. The client
		 * can retry later if this happens.
		 */
		LIBRARY_BUSY(ErrorType.BUSY),

		/**
		 * The client tried to log in using a password, but it was incorrect. The user should be asked the password
		 * again.
		 */
		LOGIN_INVALID_PASSWORD(ErrorType.MESSAGE_INVALID_CONTENTS),

		/**
		 * The client tried to log in using a token, but it was incorrect. The user should be asked the password.
		 * Optionally, a different message warning about potential unauthorized access may be included.
		 */
		LOGIN_INVALID_TOKEN(ErrorType.MESSAGE_INVALID_CONTENTS),

		/**
		 * The client tried to log in with a device ID which does not exist. It should try logging back in sending its
		 * hostname instead, and let go of its previous device id.
		 */
		LOGIN_DEVICE_DOES_NOT_EXIST(ErrorType.MESSAGE_INVALID_CONTENTS);

		public final ErrorType type;

		ErrorId(ErrorType type) {
			this.type = type;
		}
	}
}
