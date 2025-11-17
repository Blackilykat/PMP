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

package dev.blackilykat.pmp.client.gui.menubar.connection;

import dev.blackilykat.pmp.client.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JMenuItem;

public class ConnectMenuItem extends JMenuItem {
	private static final Logger LOGGER = LogManager.getLogger(ConnectMenuItem.class);

	public ConnectMenuItem() {
		super("Connect");

		addActionListener(e -> {
			Server.connect();
		});

		Server.EVENT_CONNECTED.register(_ -> {
			setEnabled(false);
		});

		Server.EVENT_DISCONNECTED.register(_ -> {
			setEnabled(true);
		});
	}
}
