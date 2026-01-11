/*
 * Copyright (C) 2026 Blackilykat and contributors
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

package dev.blackilykat.pmp.client.gui.util;

import dev.blackilykat.pmp.client.gui.Theme;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Color;

@SuppressWarnings("MagicConstant")
public class ThemedLabel extends JLabel {
	public ThemedLabel(String text, Icon icon, int horizontalAlignment) {
		super(text, icon, horizontalAlignment);
	}

	public ThemedLabel(String text, int horizontalAlignment) {
		super(text, horizontalAlignment);
	}

	public ThemedLabel(String text) {
		super(text);
	}

	public ThemedLabel(Icon image, int horizontalAlignment) {
		super(image, horizontalAlignment);
	}

	public ThemedLabel(Icon image) {
		super(image);
	}

	public ThemedLabel() {
		super();
	}

	@Override
	public Color getForeground() {
		return Theme.selected.text;
	}
}
