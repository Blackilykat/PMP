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

import dev.blackilykat.pmp.Filter;
import dev.blackilykat.pmp.FilterOption;
import dev.blackilykat.pmp.client.gui.util.ThemedLabel;
import dev.blackilykat.pmp.client.gui.util.ThemedVerticalScrollPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

public class FiltersPanel extends JPanel {
	private static final Logger LOGGER = LogManager.getLogger(FiltersPanel.class);
	private final JPanel noFiltersPanel = new JPanel() {
		@Override
		public Color getBackground() {
			return FiltersPanel.this.getBackground();
		}
	};
	private final Component topSpacing = Box.createRigidArea(new Dimension(0, 13));

	public FiltersPanel() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));


		noFiltersPanel.setLayout(new BoxLayout(noFiltersPanel, BoxLayout.PAGE_AXIS));
		JLabel label = new ThemedLabel("You have no filters. Right click to add one.");
		label.setForeground(Theme.selected.text);
		label.setFont(new Font("Source Sans Pro", Font.PLAIN, 14));
		label.setAlignmentX(JLabel.CENTER_ALIGNMENT);

		noFiltersPanel.add(Box.createVerticalGlue());
		noFiltersPanel.add(label);
		noFiltersPanel.add(Box.createVerticalGlue());


		add(noFiltersPanel);

		add(new FilterPanel("Hello"));
		add(new FilterPanel("Artist"));
		add(new FilterPanel("Album"));
	}

	public void add(FilterPanel filterPanel) {
		Component[] c = getComponents();
		if(c.length == 1 && c[0] == noFiltersPanel) {
			remove(0);
			super.add(topSpacing);
		}

		super.add(filterPanel);
	}

	public void remove(FilterPanel filterPanel) {
		super.remove(filterPanel);

		if(getComponents().length == 1) {
			super.remove(topSpacing);
			add(noFiltersPanel);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 1000);
	}

	@Override
	public Color getBackground() {
		return Theme.selected.panelBackground;
	}

	public static class FilterPanel extends JPanel {
		private JLabel name;
		private JPanel mainBox;
		private JPanel content;

		public FilterPanel(String label) {
			name = new ThemedLabel(label);
			name.setFont(new Font("Source Sans Pro", Font.PLAIN, 20));

			JPanel nameContainer = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					g.setColor(Theme.selected.filterPanelBackground);
					int w = getWidth();
					int h = getHeight();
					g.fillRoundRect(0, 0, w, h, 20, 20);
					g.fillRect(0, h / 2, w, h / 2);
				}

				@Override
				public Dimension getPreferredSize() {
					Dimension ns = name.getSize();
					return new Dimension(ns.width + 20, ns.height + 5);
				}
			};

			nameContainer.add(name, BorderLayout.CENTER);

			mainBox = new JPanel() {
				@Override
				public Color getBackground() {
					return Theme.selected.filterPanelBackground;
				}
			};

			SpringLayout mainBoxLayout = new SpringLayout();
			mainBox.setLayout(mainBoxLayout);


			content = new JPanel() {
				@Override
				public Color getBackground() {
					return Theme.selected.filterPanelBackground;
				}
			};

			content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));

			ThemedVerticalScrollPane scrollPane = new ThemedVerticalScrollPane(content);

			mainBox.add(scrollPane);

			mainBoxLayout.putConstraint(SpringLayout.NORTH, scrollPane, 5, SpringLayout.NORTH, mainBox);
			mainBoxLayout.putConstraint(SpringLayout.WEST, scrollPane, 0, SpringLayout.WEST, mainBox);
			mainBoxLayout.putConstraint(SpringLayout.EAST, scrollPane, 0, SpringLayout.EAST, mainBox);
			mainBoxLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -5, SpringLayout.SOUTH, mainBox);


			SpringLayout layout = new SpringLayout();
			setLayout(layout);

			add(nameContainer);
			add(mainBox);

			layout.putConstraint(SpringLayout.NORTH, nameContainer, 5, SpringLayout.NORTH, this);
			layout.putConstraint(SpringLayout.WEST, nameContainer, 5, SpringLayout.WEST, this);

			layout.putConstraint(SpringLayout.SOUTH, mainBox, 0, SpringLayout.SOUTH, this);
			layout.putConstraint(SpringLayout.WEST, mainBox, 0, SpringLayout.WEST, this);
			layout.putConstraint(SpringLayout.EAST, mainBox, 0, SpringLayout.EAST, this);
			layout.putConstraint(SpringLayout.NORTH, mainBox, -1, SpringLayout.SOUTH, nameContainer);

			content.add(new OptionButton(new FilterOption(Filter.OPTION_EVERYTHING)));
			content.add(new OptionButton(new FilterOption("Value 1")));
			content.add(new OptionButton(new FilterOption("Value 2")));
			content.add(new OptionButton(new FilterOption("Value 3")));
			content.add(new OptionButton(new FilterOption("Value 4")));
			content.add(new OptionButton(new FilterOption("Value 5")));
			content.add(new OptionButton(new FilterOption("Value 6")));
			content.add(new OptionButton(new FilterOption("Value 7")));
			content.add(new OptionButton(new FilterOption("Value 8")));
			content.add(new OptionButton(new FilterOption(Filter.OPTION_UNKNOWN)));
		}

		@Override
		public Color getBackground() {
			return Theme.selected.panelBackground;
		}

		@Override
		public Dimension getMaximumSize() {
			return new Dimension(Integer.MAX_VALUE, 350);
		}

		public static class OptionButton extends JButton {
			private static final Random random = new Random();
			private FilterOption.State state;
			private boolean clicked = false;

			public OptionButton(FilterOption option) {
				super(switch(option.value) {
					case Filter.OPTION_EVERYTHING -> "All";
					case Filter.OPTION_UNKNOWN -> "Unknown";
					default -> option.value;
				});

				setBorder(BorderFactory.createEmptyBorder());

				if(option.state != null) {
					state = option.state;
				}

				state = switch(random.nextInt(3)) {
					case 0 -> FilterOption.State.NONE;
					case 1 -> FilterOption.State.POSITIVE;
					default -> FilterOption.State.NEGATIVE;
				};


				setUI(new BasicButtonUI() {
					@Override
					protected void paintFocus(Graphics g, AbstractButton b, Rectangle v, Rectangle t, Rectangle i) {
					}
				});

				addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						clicked = true;
					}

					@Override
					public void mouseReleased(MouseEvent e) {
						clicked = false;
					}
				});

				revalidate();
				repaint();
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(getParent().getParent().getWidth(), 40);
			}

			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			}

			@Override
			public Dimension getMinimumSize() {
				return getPreferredSize();
			}

			@Override
			public Color getForeground() {
				return Theme.selected.text;
			}

			@Override
			public Color getBackground() {
				Color base = switch(state) {
					case NONE -> Theme.selected.filterOptionBackground;
					case POSITIVE -> Theme.selected.filterOptionBackgroundPositive;
					case NEGATIVE -> Theme.selected.filterOptionBackgroundNegative;
					case null -> Theme.selected.filterOptionBackground;
				};

				Point mouse = getMousePosition();
				if(clicked) {
					return Theme.selected.getClicked(base);
				} else if(mouse != null && contains(mouse)) {
					return Theme.selected.getHovered(base);
				} else {
					return base;
				}
			}

			@Override
			public Font getFont() {
				return new Font("Source Sans Pro", Font.PLAIN, 18);
			}
		}
	}
}
