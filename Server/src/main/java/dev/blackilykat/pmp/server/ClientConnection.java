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

package dev.blackilykat.pmp.server;

import dev.blackilykat.pmp.PMPConnection;

import java.io.IOException;
import java.net.Socket;

/// PMP Connection with a client.
public class ClientConnection extends PMPConnection {

	/// Counter used to assign a unique name to each connection for logging.
	private static int clientID = 1;

	/// The device this client has logged in as, or null if it has not logged in (yet).
	public Device device = null;

	public ClientConnection(Socket socket) throws IOException {
		super(socket, "Client " + clientID++);
	}
}
