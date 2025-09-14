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

import javax.swing.JFrame;
import javax.swing.SpringLayout;
import java.awt.Container;
import java.awt.Dimension;

public class MainWindow extends JFrame {
	private static final Dimension MIN_SIZE = new Dimension(500, 500);

	public MainWindow() {
		super("PMP");
		Container content = getContentPane();
		setMinimumSize(MIN_SIZE);

		Playbar playbar = new Playbar();
		content.add(playbar);

		FiltersPanel filters = new FiltersPanel();
		content.add(filters);

		TracksPanel tracks = new TracksPanel();
		content.add(tracks);

		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);

		layout.putConstraint(SpringLayout.NORTH, playbar, -140, SpringLayout.SOUTH, content);
		layout.putConstraint(SpringLayout.SOUTH, playbar, 0, SpringLayout.SOUTH, content);
		layout.putConstraint(SpringLayout.EAST, playbar, 0, SpringLayout.EAST, content);
		layout.putConstraint(SpringLayout.WEST, playbar, 0, SpringLayout.WEST, content);

		layout.putConstraint(SpringLayout.SOUTH, filters, 0, SpringLayout.NORTH, playbar);
		layout.putConstraint(SpringLayout.NORTH, filters, 0, SpringLayout.NORTH, content);
		layout.putConstraint(SpringLayout.WEST, filters, 0, SpringLayout.WEST, content);

		layout.putConstraint(SpringLayout.WEST, tracks, 0, SpringLayout.EAST, filters);
		layout.putConstraint(SpringLayout.SOUTH, tracks, 0, SpringLayout.NORTH, playbar);
		layout.putConstraint(SpringLayout.EAST, tracks, 0, SpringLayout.EAST, content);
		layout.putConstraint(SpringLayout.NORTH, tracks, 0, SpringLayout.NORTH, content);

		content.setBackground(Theme.DEFAULT.tracklistBackground);

		// prevent white flash on startup
		setBackground(Theme.DEFAULT.tracklistBackground);
	}

	@Override
	public Dimension getMinimumSize() {
		return MIN_SIZE;
	}

	public static void main(String[] args) {
		System.setProperty("awt.useSystemAAFontSettings", "lcd");
		MainWindow mainWindow = new MainWindow();

		mainWindow.setDefaultCloseOperation(EXIT_ON_CLOSE);
		mainWindow.setVisible(true);
	}
}
