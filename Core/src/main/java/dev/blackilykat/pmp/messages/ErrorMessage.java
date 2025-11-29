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

/**
 * Message used to indicate that a programming error has manifested somewhere, likely by the receiving side.
 * <p>There is no expected behavior upon receiving this, as it ideally never gets encountered at all outside of
 * development. This does not mean the expected behavior is to not react: there may be attempts at recovering the error
 * such as reconnecting.
 * <p>Direction: Bidirectional (C2S, S2C)
 */
public class ErrorMessage extends Message {
	public static final String MESSAGE_TYPE = "Error";
	/**
	 * Human-readable info message about what happened.
	 */
	public String info;

	public ErrorMessage(String info) {
		this.info = info;
	}
}
