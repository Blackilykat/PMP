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

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.DomProcessor;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import dev.blackilykat.pmp.client.gui.svg.CustomColorsProcessor;

import java.awt.Color;

public class Theme {
	public static final Theme DEFAULT = new Theme();
	public static Theme selected = DEFAULT;

	public Color panelBackground = new Color(0x343434);
	public Color text = new Color(0xb8b8b8);
	public Color tracklistBackground = new Color(0x252525);

	public Color buttonBackground = new Color(0x151515);
	public Color filterPanelBackground = new Color(0x1d1d1d);

	public Color filterOptionBackground = new Color(0x262626);
	public Color filterOptionBackgroundPositive = new Color(0x24432A);
	public Color filterOptionBackgroundNegative = new Color(0x4E1C1C);

	public Color scrollBarColor = new Color(0x66FFFFFF, true);

	public Color hover = new Color(0x0CFFFFFF, true);
	public Color clicked = new Color(0x1AFFFFFF, true);

	public int scrollBarWidth = 8;

	public int albumArtRadius = 20;

	public int buttonIconSize = 25;

	public int headerPadding = 10;
	public int headerSeparatorWidth = 2;
	public int headerSeparatorVerticalSpacing = 7;

	public int trackPlayIconPadding = 15;
	public int trackPlayIconSize = 25;

	public SVGDocument playIcon;
	public SVGDocument pauseIcon;
	public SVGDocument nextIcon;
	public SVGDocument previousIcon;
	public SVGDocument repeatAllIcon;
	public SVGDocument repeatOffIcon;
	public SVGDocument repeatTrackIcon;
	public SVGDocument shuffleOffIcon;
	public SVGDocument shuffleOnIcon;

	public Theme() {
		SVGLoader loader = new SVGLoader();
		CustomColorsProcessor processor = new CustomColorsProcessor(text);
		playIcon = loadThemedSVG("play.svg", processor, loader);
		pauseIcon = loadThemedSVG("pause.svg", processor, loader);
		nextIcon = loadThemedSVG("next.svg", processor, loader);
		previousIcon = loadThemedSVG("previous.svg", processor, loader);
		repeatAllIcon = loadThemedSVG("repeat-all.svg", processor, loader);
		repeatOffIcon = loadThemedSVG("repeat-off.svg", processor, loader);
		repeatTrackIcon = loadThemedSVG("repeat-track.svg", processor, loader);
		shuffleOffIcon = loadThemedSVG("shuffle-off.svg", processor, loader);
		shuffleOnIcon = loadThemedSVG("shuffle-on.svg", processor, loader);
	}

	public Color getHovered(Color color) {
		double hm = hover.getAlpha() / 255.0;
		double om = 1 - hm;

		return new Color((int) (color.getRed() * om + hover.getRed() * hm),
				(int) (color.getGreen() * om + hover.getGreen() * hm),
				(int) (color.getBlue() * om + hover.getBlue() * hm), color.getAlpha());
	}

	public Color getClicked(Color color) {
		double hm = clicked.getAlpha() / 255.0;
		double om = 1 - hm;

		return new Color((int) (color.getRed() * om + clicked.getRed() * hm),
				(int) (color.getGreen() * om + clicked.getGreen() * hm),
				(int) (color.getBlue() * om + clicked.getBlue() * hm), color.getAlpha());
	}

	@SuppressWarnings("DataFlowIssue")
	private static SVGDocument loadThemedSVG(String name, DomProcessor processor, SVGLoader loader) {
		return loader.load(Theme.class.getResource("/" + name),
				LoaderContext.builder().preProcessor(processor).build());
	}
}
