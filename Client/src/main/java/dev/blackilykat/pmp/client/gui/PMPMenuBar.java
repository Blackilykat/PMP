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

package dev.blackilykat.pmp.client.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.awt.event.ActionListener;

public class PMPMenuBar extends JMenuBar {
	private static final Logger LOGGER = LogManager.getLogger(PMPMenuBar.class);

	public PMPMenuBar() {
		add(menu("Debug", item("Add debug log...", e -> {
			String res = JOptionPane.showInputDialog(null, "Enter the log...", "Insert debug log",
					JOptionPane.PLAIN_MESSAGE);
			if(res != null) {
				LOGGER.warn("User-entered log: {}", res);
			}
		})));
	}

	private static JMenu menu(String text, JMenuItem... items) {
		JMenu menu = new JMenu(text);
		for(JMenuItem item : items) {
			menu.add(item);
		}
		return menu;
	}

	private static JMenuItem item(String text, ActionListener actionListener) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(actionListener);
		return item;
	}
}
