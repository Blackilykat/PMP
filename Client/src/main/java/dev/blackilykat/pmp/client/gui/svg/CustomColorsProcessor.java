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

package dev.blackilykat.pmp.client.gui.svg;

import com.github.weisj.jsvg.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.parser.DomElement;
import com.github.weisj.jsvg.parser.DomProcessor;

import java.awt.Color;
import java.util.UUID;

public class CustomColorsProcessor implements DomProcessor {
	private Color color;

	public CustomColorsProcessor(Color color) {
		this.color = color;
	}

	@Override
	public void process(DomElement root) {
		processImpl(root);
		root.children().forEach(this::process);
	}

	private void processImpl(DomElement element) {

		String uniqueIdForDynamicColor = UUID.randomUUID().toString();

		element.document().registerNamedElement(uniqueIdForDynamicColor, (SimplePaintSVGPaint) () -> color);

		element.setAttribute("fill", uniqueIdForDynamicColor);
	}
}
