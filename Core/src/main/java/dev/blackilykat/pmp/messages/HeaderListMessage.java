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

import java.util.ArrayList;
import java.util.List;


/**
 * The list of track headers. These must be shared between devices to communicate how to sort the library.
 */
public class HeaderListMessage extends Message {
	public static final String MESSAGE_TYPE = "HeaderList";

	public List<Header> headers = new ArrayList<>();

	public HeaderListMessage() {
	}

	public record Header(int id, String key, String name) {}
}
