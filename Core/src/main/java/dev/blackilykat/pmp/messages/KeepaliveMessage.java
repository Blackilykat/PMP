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

import dev.blackilykat.pmp.PMPConnection;

/**
 * Message used to keep the connection alive. A keepalive should be sent every {@value #KEEPALIVE_MS} milliseconds, and
 * the connection will be killed after <b>approximately</b> {@value #KEEPALIVE_MAX_MS} milliseconds.
 * <p>
 * This message is handled by {@link PMPConnection}.
 */
public class KeepaliveMessage extends Message {
	public static final long KEEPALIVE_MS = 10_000;
	public static final long KEEPALIVE_MAX_MS = 30_000;

	public static final String MESSAGE_TYPE = "Keepalive";

	public KeepaliveMessage() {
	}
}
