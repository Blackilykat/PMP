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

/// Any message that responds to a [Request]. There may be multiple responses for the same request if
/// [#isLastResponse] is overridden in the Response's class.
public abstract class Response extends Message {
	/// The id of the request this response is responding to.
	public Integer requestId;

	public Response(Integer requestId) {
		super();
		this.requestId = requestId;
	}

	/// Returns whether this is expected to be the last response to this request or if there are more after this.
	///
	/// All overriding methods must specify the [JsonIgnore] annotation.
	///
	/// @return whether this is expected to be the last response to this request or if there are more after this.
	@JsonIgnore
	public boolean isLastResponse() {
		return true;
	}
}
