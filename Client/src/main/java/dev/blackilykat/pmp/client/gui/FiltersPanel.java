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

package dev.blackilykat.pmp.client.gui;

import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Filter;
import dev.blackilykat.pmp.client.FilterOption;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.gui.util.GUIUtils;
import dev.blackilykat.pmp.client.gui.util.ThemedLabel;
import dev.blackilykat.pmp.client.gui.util.ThemedVerticalScrollPane;
import dev.blackilykat.pmp.event.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SpringLayout;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
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

		for(Filter filter : ClientStorage.MAIN.filters.get()) {
			add(new FilterPanel(filter));
		}
		Library.EVENT_FILTER_ADDED.register(filter -> {
			GUIUtils.runOnSwingThread(() -> {
				add(new FilterPanel(filter));
			});
		});

		Library.EVENT_FILTER_REMOVED.register(filter -> {
			GUIUtils.runOnSwingThread(() -> {
				for(Component component : getComponents()) {
					if(!(component instanceof FilterPanel fp)) {
						continue;
					}

					if(fp.filter != filter) {
						continue;
					}

					remove(fp);
				}
			});
		});

		Library.EVENT_FILTER_MOVED.register(event -> {
			Filter filter = event.filter();
			int pos = event.newPosition();

			GUIUtils.runOnSwingThread(() -> {
				FilterPanel panel = null;
				for(Component component : getComponents()) {
					if(!(component instanceof FilterPanel fp)) {
						continue;
					}

					if(fp.filter != filter) {
						continue;
					}

					panel = fp;
					remove(fp);
				}

				if(panel == null) {
					LOGGER.error("FiltersPanel#<init>: panel is null, this should be unreachable");
					return;
				}

				int componentIndex = 0, filterIndex = 0;
				for(Component component : getComponents()) {
					componentIndex++;
					if(!(component instanceof FilterPanel fp)) {
						continue;
					}

					if(filterIndex >= pos) {
						componentIndex--;
						break;
					}

					filterIndex++;
				}

				super.add(panel, componentIndex);
				panel.revalidate();
				revalidate();
				repaint();
			});
		});

		JPopupMenu menu = new JPopupMenu();
		menu.add(buildAddFilterMenuItem());
		addMouseListener(GUIUtils.createPopupListener(menu, this));
	}

	public void add(FilterPanel filterPanel) {
		Component[] c = getComponents();
		if(c.length == 1 && c[0] == noFiltersPanel) {
			remove(0);
			super.add(topSpacing);
		}

		super.add(filterPanel);

		filterPanel.mainBox.revalidate();
		filterPanel.name.revalidate();
		filterPanel.content.revalidate();
		filterPanel.revalidate();

		revalidate();
		repaint();
	}

	public void remove(FilterPanel filterPanel) {
		super.remove(filterPanel);

		if(getComponents().length == 1) {
			super.remove(topSpacing);
			add(noFiltersPanel);
		}

		revalidate();
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 1000);
	}

	@Override
	public Color getBackground() {
		return Theme.selected.panelBackground;
	}

	public static JMenuItem buildAddFilterMenuItem() {
		JMenuItem item = new JMenuItem("Add filter...");
		item.addActionListener(_ -> {
			String res = JOptionPane.showInputDialog(null, "Choose filter key", "Add a filter",
					JOptionPane.PLAIN_MESSAGE);
			if(res == null) {
				return;
			}

			Filter filter = new Filter(res);
			Library.addFilter(filter);
		});

		return item;
	}

	public static JMenuItem buildRemoveFilterMenuItem(Filter filter) {
		JMenuItem item = new JMenuItem("Remove " + filter.key);
		item.addActionListener(_ -> {
			Library.removeFilter(filter);
		});
		return item;
	}

	public static JMenuItem buildMoveFilterUpMenuItem(Filter filter) {
		JMenuItem item = new JMenuItem("Move " + filter.key + " up");
		item.addActionListener(_ -> {
			int pos = ClientStorage.MAIN.filters.indexOf(filter);
			if(pos < 1) {
				return;
			}

			Library.moveFilter(filter, pos - 1);
		});
		return item;
	}

	public static JMenuItem buildMoveFilterDownMenuItem(Filter filter) {
		JMenuItem item = new JMenuItem("Move " + filter.key + " down");
		item.addActionListener(_ -> {
			int pos = ClientStorage.MAIN.filters.indexOf(filter);
			if(pos >= ClientStorage.MAIN.filters.size() - 1 || pos < 0) {
				return;
			}

			Library.moveFilter(filter, pos + 1);
		});
		return item;
	}

	public static class FilterPanel extends JPanel {
		public final Filter filter;
		private JLabel name;
		private JPanel mainBox;
		private JPanel content;

		public FilterPanel(Filter filter) {
			this.filter = filter;
			String label;
			{
				char[] keyChars = filter.key.toCharArray();

				boolean capitalize = true;
				for(int i = 0; i < keyChars.length; i++) {
					if(capitalize) {
						keyChars[i] = Character.toUpperCase(keyChars[i]);
					}
					capitalize = keyChars[i] == ' ';
				}

				label = new String(keyChars);
			}
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
					Dimension ns = name.getPreferredSize();
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

			for(FilterOption option : filter.getOptions()) {
				addOption(option);
			}

			filter.eventOptionAdded.register(event -> {
				int index = event.index();
				FilterOption option = event.option();

				GUIUtils.runOnSwingThread(() -> {
					addOption(index, option);
				});
			});

			filter.eventOptionRemoved.register(event -> {
				FilterOption option = event.option();

				GUIUtils.runOnSwingThread(() -> {
					removeOption(option);
				});
			});

			JPopupMenu menu = new JPopupMenu();
			menu.add(buildAddFilterMenuItem());
			menu.add(buildRemoveFilterMenuItem(filter));
			menu.add(buildMoveFilterUpMenuItem(filter));
			menu.add(buildMoveFilterDownMenuItem(filter));
			addMouseListener(GUIUtils.createPopupListener(menu, this));
		}

		public void addOption(FilterOption option) {
			OptionButton btn = new OptionButton(option);
			btn.register();

			content.add(btn);

			content.revalidate();
			content.repaint();
		}

		public void addOption(int index, FilterOption option) {
			OptionButton btn = new OptionButton(option);
			btn.register();

			content.add(btn, index);

			content.revalidate();
			content.repaint();
		}

		public void removeOption(FilterOption option) {
			for(Component component : content.getComponents()) {
				if(!(component instanceof OptionButton button)) {
					continue;
				}

				if(button.option != option) {
					continue;
				}

				content.remove(component);
				break;
			}

			content.revalidate();
			content.repaint();
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
			private final FilterOption option;
			private final Listener<Filter.OptionChangedStateEvent> listener;

			private boolean clicked = false;

			public OptionButton(FilterOption option) {
				this.option = option;
				super(switch(option.value) {
					case Filter.OPTION_EVERYTHING -> "All";
					case Filter.OPTION_UNKNOWN -> "Unknown";
					default -> option.value;
				});
				listener = event -> {
					repaint();
				};

				setBorder(BorderFactory.createEmptyBorder());

				setUI(new BasicButtonUI());

				addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						clicked = true;
						repaint();
					}

					@Override
					public void mouseReleased(MouseEvent e) {
						clicked = false;

						if(e.getButton() == MouseEvent.BUTTON1) {
							option.setState(switch(option.getState()) {
								case NONE, NEGATIVE -> FilterOption.State.POSITIVE;
								case POSITIVE -> FilterOption.State.NONE;
							});
						} else if(e.getButton() == MouseEvent.BUTTON3) {
							option.setState(switch(option.getState()) {
								case NONE, POSITIVE -> FilterOption.State.NEGATIVE;
								case NEGATIVE -> FilterOption.State.NONE;
							});
						}

						repaint();
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
				Color base = switch(option.getState()) {
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

			public void register() {
				option.eventChangedState.register(listener);
			}

			public void unregister() {
				option.eventChangedState.unregister(listener);
			}
		}
	}
}
