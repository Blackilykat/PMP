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

import dev.blackilykat.pmp.client.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.SpringLayout;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow extends JFrame {
	public final static Logger LOGGER = LogManager.getLogger(MainWindow.class);
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

		setJMenuBar(new PMPMenuBar());
	}

	@Override
	public Dimension getMinimumSize() {
		return MIN_SIZE;
	}

	public static void main(String[] args) {
		System.setProperty("awt.useSystemAAFontSettings", "lcd");

		setLookAndFeelDefaults();

		MainWindow mainWindow = new MainWindow();

		mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				Main.shutdown(true);
			}
		});

		mainWindow.setVisible(true);
	}


	public static void setLookAndFeelDefaults() {
		UIDefaults defaults = UIManager.getLookAndFeelDefaults();
		defaults.put("MenuBar.background", Theme.selected.menuBarBackground);
		defaults.put("MenuBar.foreground", Theme.selected.text);
		defaults.put("MenuBar.border", BorderFactory.createLineBorder(Theme.selected.menuBarBackground, 4));

		defaults.put("MenuItem.background", Theme.selected.menuBarBackground);
		defaults.put("MenuItem.foreground", Theme.selected.text);
		defaults.put("MenuItem.selectionBackground", Theme.selected.getHovered(Theme.selected.menuBarBackground));
		defaults.put("MenuItem.selectionForeground", Theme.selected.text);
		defaults.put("MenuItem.font", new Font("Source Sans Pro", Font.PLAIN, 16));
		defaults.put("MenuItem.border", BorderFactory.createEmptyBorder());

		defaults.put("Menu.background", Theme.selected.menuBarBackground);
		defaults.put("Menu.foreground", Theme.selected.text);
		defaults.put("Menu.selectionBackground", Theme.selected.getClicked(Theme.selected.menuBarBackground));
		defaults.put("Menu.selectionForeground", Theme.selected.text);
		defaults.put("Menu.border", BorderFactory.createEmptyBorder());
		defaults.put("Menu.font", new Font("Source Sans Pro", Font.PLAIN, 16));

		defaults.put("PopupMenu.background", Theme.selected.menuBarBackground);
		defaults.put("PopupMenu.border", BorderFactory.createEmptyBorder());

		defaults.put("Panel.background", Theme.selected.panelBackground);

		defaults.put("Label.font", new Font("Source Sans Pro", Font.PLAIN, 16));

		defaults.put("Button.background", Theme.selected.buttonBackground);
		defaults.put("Button.foreground", Theme.selected.text);
		defaults.put("Button.select", Theme.selected.getClicked(Theme.selected.buttonBackground));
		defaults.put("Button.focus", Theme.selected.panelBackground);
		defaults.put("Button.border", BorderFactory.createEmptyBorder(4, 8, 4, 8));
		defaults.put("Button.font", new Font("Source Sans Pro", Font.PLAIN, 16));

		defaults.put("TextField.background", Theme.selected.buttonBackground);
		defaults.put("TextField.foreground", Theme.selected.text);
		defaults.put("TextField.selectionBackground", Theme.selected.text);
		defaults.put("TextField.selectionForeground", Theme.selected.buttonBackground);
		defaults.put("TextField.caretForeground", Theme.selected.text);
		defaults.put("TextField.border", BorderFactory.createEmptyBorder(4, 4, 4, 4));
		defaults.put("TextField.font", new Font("Source Sans Pro", Font.PLAIN, 16));

		defaults.put("OptionPane.messageAreaBackground", Theme.selected.panelBackground);
		defaults.put("OptionPane.buttonAreaBackground", Theme.selected.panelBackground);
		defaults.put("OptionPane.border", BorderFactory.createEmptyBorder(10, 15, 10, 15));
		defaults.put("OptionPane.messageForeground", Theme.selected.text);
		defaults.put("OptionPane.foreground", Theme.selected.text);
		defaults.put("OptionPane.background", Theme.selected.panelBackground);

		defaults.put("ComboBox.background", Theme.selected.buttonBackground);
		defaults.put("ComboBox.foreground", Theme.selected.text);
		defaults.put("ComboBox.buttonBackground", Theme.selected.buttonBackground);
		defaults.put("ComboBox.buttonHighlight", Theme.selected.text);
		defaults.put("ComboBox.selectionBackground", Theme.selected.getHovered(Theme.selected.buttonBackground));
		defaults.put("ComboBox.selectionForeground", Theme.selected.text);
		defaults.put("ComboBox.font", new Font("Source Sans Pro", Font.PLAIN, 16));
		defaults.put("ComboBox.border", BorderFactory.createEmptyBorder(4, 8, 4, 8));

		defaults.put("ComboBox.editor.background", Theme.selected.buttonBackground);
		defaults.put("ComboBox.editor.foreground", Theme.selected.text);
		defaults.put("ComboBox.editor.border", BorderFactory.createEmptyBorder(4, 4, 4, 4));

		defaults.put("CheckBox.background", Theme.selected.panelBackground);
		defaults.put("CheckBox.foreground", Theme.selected.text);
		defaults.put("CheckBox.selectionBackground", Theme.selected.getClicked(Theme.selected.panelBackground));
		defaults.put("CheckBox.selectionForeground", Theme.selected.text);
		defaults.put("CheckBox.font", new Font("Source Sans Pro", Font.PLAIN, 16));
		defaults.put("CheckBox.border", BorderFactory.createEmptyBorder(4, 4, 4, 4));

		LOGGER.debug("Defaults {}", defaults);
	}
}
