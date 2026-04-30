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

package dev.blackilykat.pmp.client.gui.util;

import dev.blackilykat.pmp.client.gui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

public class ThemedVerticalScrollPane extends JScrollPane {
	public ThemedVerticalScrollPane(JComponent component) {
		super(component);

		setLayout(new ScrollPaneLayout() {
			@Override
			public void layoutContainer(Container parent) {
				JScrollPane scrollPane = (JScrollPane) parent;

				Rectangle scrollBounds = scrollPane.getBounds();
				scrollBounds.x = scrollBounds.y = 0;

				Rectangle barBounds = new Rectangle();
				barBounds.width = Theme.selected.scrollBarWidth;
				barBounds.height = scrollBounds.height;
				barBounds.x = scrollBounds.x + scrollBounds.width - barBounds.width;
				barBounds.y = scrollBounds.y;

				if(viewport != null) {
					viewport.setBounds(scrollBounds);
				}
				if(vsb != null) {
					vsb.setVisible(true);
					vsb.setBounds(barBounds);
				}
			}
		});

		JScrollBar scrollBar = getVerticalScrollBar();

		setComponentZOrder(scrollBar, 0);
		setComponentZOrder(getViewport(), 1);

		scrollBar.setOpaque(false);
		scrollBar.setUnitIncrement(8);
		scrollBar.setUI(new BasicScrollBarUI() {
			@Override
			protected JButton createDecreaseButton(int orientation) {
				return createIncreaseButton(orientation);
			}

			@Override
			protected JButton createIncreaseButton(int orientation) {
				return new JButton() {
					@Override
					public Dimension getPreferredSize() {
						return new Dimension();
					}
				};
			}

			@Override
			protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
			}

			@Override
			protected void paintThumb(Graphics g, JComponent c, Rectangle b) {
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(Theme.selected.scrollBarColor);
				g.fillRoundRect(b.x, b.y, b.width, b.height, Theme.selected.scrollBarWidth,
						Theme.selected.scrollBarWidth);
			}

			@Override
			protected void setThumbBounds(int x, int y, int width, int height) {
				super.setThumbBounds(x, y, width, height);
				scrollBar.repaint();
			}
		});

		setBorder(BorderFactory.createEmptyBorder());

		setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
	}

	@Override
	public boolean isOptimizedDrawingEnabled() {
		return false;
	}
}
