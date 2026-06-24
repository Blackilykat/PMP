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

import dev.blackilykat.pmp.Action;

/// An action which has just been performed by another client.
///
/// Direction: S2C
///
/// @see Action
public class ActionMessage extends Message {
	public static final String MESSAGE_TYPE = "Action";

	/// The action itself with all details needed (except file contents) to apply it to the library.
	public Action action;

	/// The id of the action assigned incrementally by the server.
	///
	/// Action ids are stored by clients to determine when to ask for missing actions.
	///
	/// @see GetActionsRequest
	public int id;

	public ActionMessage(Action action, int id) {
		this.action = action;
		this.id = id;
	}
}
