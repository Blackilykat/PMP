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

package dev.blackilykat.pmp.client.gui.menubar.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class AddDebugLogMenuItem extends JMenuItem {
	private static final Logger LOGGER = LogManager.getLogger(AddDebugLogMenuItem.class);

	public AddDebugLogMenuItem() {
		super("Add debug log...");

		addActionListener(e -> {
			String res = JOptionPane.showInputDialog(null, "Enter the log...", "Insert debug log",
					JOptionPane.PLAIN_MESSAGE);
			if(res != null) {
				LOGGER.warn("User-entered log: {}", res);
			}
		});
	}
}
