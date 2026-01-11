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

package dev.blackilykat.pmp.client.gui.laf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;

public class PMPLabelUI extends BasicLabelUI {
	private static final Logger LOGGER = LogManager.getLogger(PMPLabelUI.class);

	// @formatter:off
	private static final List<Character.UnicodeScript> NON_ANTI_ALIASED_SCRIPTS = List.of(
			Character.UnicodeScript.MIAO,                // example: 漀
			Character.UnicodeScript.BASSA_VAH,           // example: 櫐
			Character.UnicodeScript.PAHAWH_HMONG,        // example: 欀
			Character.UnicodeScript.MRO,                 // example: 橀
			Character.UnicodeScript.TANGUT,              // example: 濠
			Character.UnicodeScript.NUSHU,               // example: 濡
			Character.UnicodeScript.MEDEFAIDRIN,         // example: 湀
			Character.UnicodeScript.KHITAN_SMALL_SCRIPT, // example: 濤
			Character.UnicodeScript.CYPRO_MINOAN,        // example: ⾐
			Character.UnicodeScript.GURUNG_KHEMA,        // example: 愀
			Character.UnicodeScript.KIRAT_RAI,           // example: 浀
			Character.UnicodeScript.HAN                  // example: 翻
	);
	// @formatter:on

	private JLabel label;
	/**
	 * strings in this list alternate between anti aliased and not anti aliased, starting from anti aliased.
	 */
	private List<String> segments = null;

	public PMPLabelUI(JLabel label) {
		this.label = label;
		updateSegments();
		label.addPropertyChangeListener("text", _ -> {
			updateSegments();
		});
	}

	@Override
	protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {

		g.setColor(l.getForeground());
		FontMetrics fm = g.getFontMetrics();


		boolean aa = true;
		int x = textX;
		for(String segment : segments) {
			if(aa) {
				((Graphics2D) g).setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
			} else {
				((Graphics2D) g).setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_OFF);
			}

			g.drawString(segment, x, textY);
			x += fm.stringWidth(segment);

			aa = !aa;
		}
	}

	@Override
	protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
	}

	private void updateSegments() {
		String text = label.getText();
		List<StringBuilder> segments = new LinkedList<>();
		segments.add(new StringBuilder());

		{
			AtomicBoolean aa = new AtomicBoolean(true);
			text.chars().forEachOrdered(c -> {
				if(NON_ANTI_ALIASED_SCRIPTS.contains(Character.UnicodeScript.of(c)) == aa.get()) {
					segments.add(new StringBuilder());
					aa.set(!aa.get());
				}

				segments.getLast().append((char) c);
			});
		}
		this.segments = segments.stream().map(b -> b.toString()).toList();
	}

	public static PMPLabelUI createUI(JComponent c) {
		if(!(c instanceof JLabel l)) {
			throw new IllegalArgumentException("PMPLabelUI on non-JLabel element");
		}
		return new PMPLabelUI(l);
	}
}
