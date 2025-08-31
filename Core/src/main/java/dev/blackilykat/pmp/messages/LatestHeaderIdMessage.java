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
 * The last header ID. Used to ensure a client doesn't reuse the id of a previously created then deleted header.
 */
public class LatestHeaderIdMessage extends Message {
	public static final String MESSAGE_TYPE = "LatestHeaderId";

	public Integer id;

	public LatestHeaderIdMessage(Integer id) {
		this.id = id;
	}
}
