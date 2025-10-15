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

import com.github.weisj.jsvg.view.ViewBox;
import dev.blackilykat.pmp.Order;
import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Header;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Track;
import dev.blackilykat.pmp.client.gui.util.GUIUtils;
import dev.blackilykat.pmp.client.gui.util.ThemedLabel;
import dev.blackilykat.pmp.client.gui.util.ThemedVerticalScrollPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TracksPanel extends JPanel {
	private static final Logger LOGGER = LogManager.getLogger(TracksPanel.class);
	private static final int DOUBLE_CLICK_MS = 750;
	private static final int HEADER_RESIZE_RATE_LIMIT = 60;
	private static final Map<String, String> COMMON_METADATA_KEYS_LABELS = Map.of("album", "Album", "albumartist",
			"Album Artist", "artist", "Artist", "date", "Date", "duration", "Duration", "title", "Title",
			"tracknumber",
			"N°");
	private static final Map<Header.Type, Integer> INITIAL_HEADER_WIDTHS = Map.of(Header.Type.TRACKNUMBER, 50,
			Header.Type.INTEGER, 50, Header.Type.DOUBLE, 70, Header.Type.DURATION, 100, Header.Type.TITLE, 400,
			Header.Type.STRING, 300);

	private static final List<Header.Type> RIGHT_ALIGNED_TYPES = List.of(Header.Type.DOUBLE, Header.Type.INTEGER,
			Header.Type.DURATION, Header.Type.TRACKNUMBER);

	private static Map<Integer, Integer> headerWidths = new HashMap<>();
	private HeadersPanel headersPanel = new HeadersPanel();
	private ContentPanel contentPanel;

	public TracksPanel() {
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		add(headersPanel);


		contentPanel = new ContentPanel();

		ThemedVerticalScrollPane scrollPane = new ThemedVerticalScrollPane(contentPanel);

		add(scrollPane);

		layout.putConstraint(SpringLayout.NORTH, headersPanel, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, headersPanel, 0, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.WEST, headersPanel, 0, SpringLayout.WEST, this);

		layout.putConstraint(SpringLayout.NORTH, scrollPane, 0, SpringLayout.SOUTH, headersPanel);
		layout.putConstraint(SpringLayout.WEST, scrollPane, 0, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, scrollPane, 0, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, scrollPane, 0, SpringLayout.SOUTH, this);


		Library.EVENT_SORTING_HEADER_UPDATED.register(event -> {
			Header header = event.header();
			Order order = event.order();

			for(Component component : headersPanel.getComponents()) {
				if(!(component instanceof HeaderPanel headerPanel)) {
					continue;
				}

				headerPanel.setSortingOrder(header == headerPanel.header ? order : null);
			}
		});

		for(Header header : Library.getHeaders()) {
			addHeader(header);
		}

		Library.EVENT_HEADER_ADDED.register(header -> {
			addHeader(header);
		});

		Library.EVENT_HEADER_REMOVED.register(header -> {
			removeHeader(header);
		});

		updateTracks();
		Library.EVENT_SELECTED_TRACKS_UPDATED.register(event -> {
			List<Track> tracks = event.newSelection();
			updateTracks(tracks);
		});

		Player.EVENT_TRACK_CHANGE.register(event -> {
			Track track = event.track();

			for(Component component : contentPanel.getComponents()) {
				if(!(component instanceof TrackPanel trackPanel)) {
					continue;
				}
				trackPanel.setPlaying(trackPanel.track == track);
			}
		});
	}

	private void addHeader(Header header) {
		if(!headerWidths.containsKey(header.id)) {
			Integer width = INITIAL_HEADER_WIDTHS.get(header.type);
			if(width == null) {
				width = 400;
				LOGGER.error("TracksPanel#addHeader: Unexpected null initial width for header type {}, "
						+ "falling back to {}, this should be unreachable", header.type, width);
			}
			headerWidths.put(header.id, width);
		}
		HeaderPanel hp = new HeaderPanel(this, header);
		if(Library.getSortingHeader() == hp.header) {
			hp.setSortingOrder(Library.getSortingOrder());
		}
		headersPanel.add(hp);

		headersPanel.revalidate();
		headersPanel.repaint();

		updateTracks();
	}

	private void removeHeader(Header header) {
		for(Component component : headersPanel.getComponents()) {
			if(component instanceof HeaderPanel hp && hp.header == header) {
				headersPanel.remove(hp);
			} else if(component instanceof HeaderPanel.PaddingPanel pp && pp.headerPanel.header == header) {
				headersPanel.remove(pp);
			}
		}

		headersPanel.revalidate();
		headersPanel.repaint();

		updateTracks();
	}

	private void updateTracks() {
		updateTracks(Library.getSelectedTracks());
	}

	private void updateTracks(List<Track> tracks) {
		SwingUtilities.invokeLater(() -> {
			contentPanel.removeAll();
			for(Track track : tracks) {
				List<String> values = new LinkedList<>();

				for(Header header : Library.getHeaders()) {
					values.add(header.getStringValue(track));
				}

				contentPanel.add(new TrackPanel(track, Player.getTrack() == track, values));
			}
			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	@Override
	public Color getBackground() {
		return Theme.selected.tracklistBackground;
	}

	private static JMenuItem buildAddHeaderItem() {
		JMenuItem item = new JMenuItem("Add header...");
		item.addActionListener(e -> {
			JPanel panel = new JPanel();
			SpringLayout layout = new SpringLayout();
			panel.setLayout(layout);

			ThemedLabel keyLabel = new ThemedLabel("Key:");
			JComboBox<String> keyField = new JComboBox<>(COMMON_METADATA_KEYS_LABELS.keySet().toArray(new String[0]));
			keyField.setUI(new BasicComboBoxUI());
			keyField.setEditable(true);
			JCheckBox customLabelCheckbox = new JCheckBox("Apply custom label");

			ThemedLabel labelLabel = new ThemedLabel("Label:");

			JTextField labelField = new JTextField() {
				@Override
				public Color getForeground() {
					if(isEditable()) {
						return super.getForeground();
					} else {
						return Theme.selected.disabledText;
					}
				}

				@Override
				public Color getBackground() {
					if(isEditable()) {
						return super.getBackground();
					} else {
						return Theme.selected.disabledButtonBackground;
					}
				}
			};

			updateAddHeaderItemUI(customLabelCheckbox, keyField, labelField);

			customLabelCheckbox.addActionListener(evt -> {
				updateAddHeaderItemUI(customLabelCheckbox, keyField, labelField);
			});

			((JTextComponent) keyField.getEditor().getEditorComponent()).getDocument()
					.addDocumentListener(new DocumentListener() {
						@Override
						public void insertUpdate(DocumentEvent e) {
							update();
						}

						@Override
						public void removeUpdate(DocumentEvent e) {
							update();
						}

						@Override
						public void changedUpdate(DocumentEvent e) {
							update();
						}

						private void update() {
							updateAddHeaderItemUI(customLabelCheckbox, keyField, labelField);
						}
					});

			layout.putConstraint(SpringLayout.WEST, keyLabel, 0, SpringLayout.WEST, panel);
			layout.putConstraint(SpringLayout.NORTH, keyField, 0, SpringLayout.NORTH, panel);
			layout.putConstraint(SpringLayout.WEST, keyField, 5, SpringLayout.EAST, keyLabel);
			layout.putConstraint(SpringLayout.VERTICAL_CENTER, keyLabel, 0, SpringLayout.VERTICAL_CENTER, keyField);

			layout.putConstraint(SpringLayout.NORTH, customLabelCheckbox, 50, SpringLayout.SOUTH, keyField);
			layout.putConstraint(SpringLayout.WEST, customLabelCheckbox, 0, SpringLayout.WEST, keyLabel);

			layout.putConstraint(SpringLayout.WEST, labelLabel, 0, SpringLayout.WEST, customLabelCheckbox);
			layout.putConstraint(SpringLayout.NORTH, labelField, 0, SpringLayout.SOUTH, customLabelCheckbox);
			layout.putConstraint(SpringLayout.WEST, labelField, 5, SpringLayout.EAST, labelLabel);
			layout.putConstraint(SpringLayout.EAST, labelField, 200, SpringLayout.WEST, labelField);
			layout.putConstraint(SpringLayout.VERTICAL_CENTER, labelLabel, 0, SpringLayout.VERTICAL_CENTER,
					labelField);

			panel.add(keyLabel);
			panel.add(keyField);
			panel.add(customLabelCheckbox);
			panel.add(labelLabel);
			panel.add(labelField);

			panel.setPreferredSize(new Dimension(300, 230));
			int chosen = JOptionPane.showOptionDialog(null, panel, "Add new header", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, null, null);

			if(chosen == JOptionPane.CANCEL_OPTION || chosen == JOptionPane.CLOSED_OPTION) {
				return;
			}

			Header header = new Header(ClientStorage.getInstance().getAndIncrementCurrentHeaderId(),
					labelField.getText(), ((JTextComponent) keyField.getEditor().getEditorComponent()).getText());

			Library.addHeader(header);
		});
		return item;
	}

	private static void updateAddHeaderItemUI(JCheckBox customLabelCheckbox, JComboBox<String> keyField,
			JTextField labelField) {
		if(customLabelCheckbox.isSelected()) {
			labelField.setEditable(true);
		} else {
			labelField.setEditable(false);

			String label;
			String selected = ((JTextComponent) keyField.getEditor().getEditorComponent()).getText();

			if(COMMON_METADATA_KEYS_LABELS.containsKey(selected)) {
				label = COMMON_METADATA_KEYS_LABELS.get(selected);
			} else {
				StringBuilder builder = new StringBuilder();
				boolean capitalize = true;
				for(char c : selected.toCharArray()) {
					String s = Character.toString(c);


					if(capitalize) {
						builder.append(s.toUpperCase());
					} else {
						builder.append(s.toLowerCase());
					}

					capitalize = c == ' ';
				}
				label = builder.toString();
			}
			labelField.setText(label);
		}
	}


	private static JMenuItem buildRemoveHeaderItem(Header header) {
		JMenuItem item = new JMenuItem("Remove " + header.getLabel());
		item.addActionListener(e -> {
			Library.removeHeader(header);
		});
		return item;
	}

	private static class TrackPanel extends JPanel {
		public final Track track;

		private boolean playing;
		private boolean clicked = false;
		private Instant lastClick = Instant.EPOCH;

		public TrackPanel(Track track, boolean playing, List<String> strings) {
			this.track = track;
			this.playing = playing;
			super();
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

			List<Header> headers = Library.getHeaders();
			if(strings.size() != headers.size()) {
				throw new IllegalArgumentException("Expected " + headers.size() + " values");
			}

			add(Box.createRigidArea(
					new Dimension(Theme.selected.trackPlayIconPadding * 2 + Theme.selected.trackPlayIconSize, 0)));
			for(int i = 0; i < headers.size(); i++) {
				Header header = headers.get(i);
				add(new TrackDataPanel(strings.get(i), headerWidths.get(header.id),
						RIGHT_ALIGNED_TYPES.contains(header.type)));
				add(Box.createRigidArea(new Dimension(Theme.selected.headerPadding * 2, 0)));
			}

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					Instant now = Instant.now();
					if(now.toEpochMilli() - lastClick.toEpochMilli() < DOUBLE_CLICK_MS) {
						Player.play(track);
						lastClick = Instant.EPOCH;
						return;
					}
					lastClick = now;
				}

				@Override
				public void mousePressed(MouseEvent e) {
					clicked = true;
					repaint();
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					clicked = false;
					repaint();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					repaint();
				}
			});
		}

		public void updateWidths() {
			int i = 0;
			List<Header> headers = Library.getHeaders();
			for(Component component : getComponents()) {
				if(!(component instanceof TrackDataPanel dataPanel)) {
					continue;
				}

				dataPanel.setWidth(headerWidths.get(headers.get(i).id));

				i++;
			}
		}

		public void setPlaying(boolean playing) {
			this.playing = playing;
			repaint();
		}

		@Override
		public Color getBackground() {
			Color base = Theme.selected.tracklistBackground;

			Point mouse = getMousePosition();
			if(clicked) {
				return Theme.selected.getClicked(base);
			} else if(mouse != null && contains(mouse)) {
				return Theme.selected.getHovered(base);
			}
			return base;
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if(playing) {

				g.setColor(Theme.selected.text);

				int size = Theme.selected.trackPlayIconSize;

				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				Theme.selected.playIcon.render(this, (Graphics2D) g,
						new ViewBox(Theme.selected.trackPlayIconPadding, (getHeight() - size) / 2f, size, size));
			}
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(getParent().getParent().getWidth(), 55);
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}
	}

	private static class HeadersPanel extends JPanel {

		public HeadersPanel() {
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

			add(Box.createRigidArea(
					new Dimension(Theme.selected.trackPlayIconPadding * 2 + Theme.selected.trackPlayIconSize, 0)));

			JPopupMenu menu = new JPopupMenu();
			menu.add(buildAddHeaderItem());

			addMouseListener(GUIUtils.createPopupListener(menu, this));
		}

		@Override
		public Color getBackground() {
			return Theme.selected.panelBackground;
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(getParent().getWidth(), 50);
		}

		public void add(HeaderPanel panel) {
			super.add(panel);
			super.add(new HeaderPanel.PaddingPanel(panel));
		}
	}

	private static class HeaderPanel extends JPanel {
		public final TracksPanel tp;
		public final Header header;
		protected int width;
		private Order sortingOrder = null;
		private boolean clicked = false;
		private boolean hovered = false;

		public HeaderPanel(TracksPanel tp, Header header) {
			this.tp = tp;
			this.width = headerWidths.get(header.id);
			this.header = header;

			SpringLayout layout = new SpringLayout();
			setLayout(layout);

			JLabel label = new ThemedLabel(header.getLabel());
			label.setFont(new Font("Source Sans Pro", Font.PLAIN, 21));
			add(label);

			layout.putConstraint(SpringLayout.VERTICAL_CENTER, label, 0, SpringLayout.VERTICAL_CENTER, this);
			if(RIGHT_ALIGNED_TYPES.contains(header.type)) {
				layout.putConstraint(SpringLayout.EAST, label, 0, SpringLayout.EAST, this);
			} else {
				layout.putConstraint(SpringLayout.WEST, label, 0, SpringLayout.WEST, this);
			}

			JPopupMenu menu = new JPopupMenu();
			menu.add(buildAddHeaderItem());
			menu.add(buildRemoveHeaderItem(header));

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) {
						clicked = true;
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) {
						clicked = false;
						if(sortingOrder == Order.ASCENDING) {
							Library.setSorting(header, Order.DESCENDING);
						} else {
							Library.setSorting(header, Order.ASCENDING);
						}
						repaint();
					}
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					hovered = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					hovered = false;
					repaint();
				}
			});

			addMouseListener(GUIUtils.createPopupListener(menu, this));
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public void setSortingOrder(Order sortingOrder) {
			this.sortingOrder = sortingOrder;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			int s = Theme.selected.headerSortingArrowSize;
			int o = Theme.selected.headerSortingArrowDistance;
			int w = getWidth();
			int h = getHeight();

			if(sortingOrder == Order.ASCENDING) {
				g.setColor(Theme.selected.text);
				g.fillPolygon(new int[]{w / 2, (w / 2) - s, (w / 2) + s}, new int[]{h - o, h - s - o, h - s - o}, 3);
			} else if(sortingOrder == Order.DESCENDING) {
				g.setColor(Theme.selected.text);
				g.fillPolygon(new int[]{w / 2, (w / 2) - s, (w / 2) + s}, new int[]{o, o + s, o + s}, 3);
			}
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(width, getParent().getHeight());
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
		public Color getBackground() {
			Color base = Theme.selected.panelBackground;
			if(clicked) {
				return Theme.selected.getClicked(base);
			}
			if(hovered) {
				return Theme.selected.getHovered(base);
			}
			return base;
		}

		public static class PaddingPanel extends JPanel {
			public final HeaderPanel headerPanel;
			private boolean hovered = false;
			private boolean clicked = false;
			private int lastX = -1;
			private long lastWidthUpdate;

			public PaddingPanel(HeaderPanel hp) {
				this.headerPanel = hp;
				addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						if(e.getButton() == MouseEvent.BUTTON1) {
							clicked = true;
							lastX = e.getXOnScreen();
							repaint();
						}
					}

					@Override
					public void mouseReleased(MouseEvent e) {
						if(e.getButton() == MouseEvent.BUTTON1) {
							clicked = false;
							updateWidth(e.getXOnScreen(), false);
							repaint();
						}
					}

					@Override
					public void mouseEntered(MouseEvent e) {
						hovered = true;
						repaint();
					}

					@Override
					public void mouseExited(MouseEvent e) {
						hovered = false;
						repaint();
					}
				});

				addMouseMotionListener(new MouseAdapter() {
					@Override
					public void mouseDragged(MouseEvent e) {
						if(clicked) {
							updateWidth(e.getXOnScreen(), true);
						}
					}
				});

				JPopupMenu menu = new JPopupMenu();

				menu.add(buildAddHeaderItem());

				addMouseListener(GUIUtils.createPopupListener(menu, this));
			}

			private void updateWidth(int xOnScreen, boolean rateLimit) {
				long now = System.currentTimeMillis();
				boolean shouldUpdateTracks = true;
				if(rateLimit && lastWidthUpdate != 0) {
					long diff = now - lastWidthUpdate;
					if(diff < 1000.0 / HEADER_RESIZE_RATE_LIMIT) {
						shouldUpdateTracks = false;
					}
				}

				int offset = xOnScreen - lastX;

				headerWidths.put(headerPanel.header.id, headerWidths.get(headerPanel.header.id) + offset);
				headerPanel.width = headerWidths.get(headerPanel.header.id);
				headerPanel.revalidate();

				if(shouldUpdateTracks) {
					lastWidthUpdate = now;
					for(Component component : headerPanel.tp.contentPanel.getComponents()) {
						if(!(component instanceof TrackPanel trackPanel)) {
							continue;
						}

						trackPanel.updateWidths();
					}
				}

				lastX = xOnScreen;
			}

			@Override
			public Color getBackground() {
				Color base = Theme.selected.panelBackground;
				if(clicked) {
					return Theme.selected.getClicked(base);
				}
				if(hovered) {
					return Theme.selected.getHovered(base);
				}
				return base;
			}

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(Theme.selected.text);
				int width = getWidth();
				int height = getHeight();
				int sw = Theme.selected.headerSeparatorWidth;
				int svs = Theme.selected.headerSeparatorVerticalSpacing;

				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.fillRoundRect((width / 2) - sw + 1, svs, sw, height - svs * 2, sw, sw);
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(Theme.selected.headerPadding * 2, getParent().getHeight());
			}

			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			}

			@Override
			public Dimension getMinimumSize() {
				return getPreferredSize();
			}
		}
	}

	private static class ContentPanel extends JPanel {
		public ContentPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		}

		@Override
		public Color getBackground() {
			return Theme.selected.tracklistBackground;
		}
	}

	private static class TrackDataPanel extends JPanel {
		protected int width;

		public TrackDataPanel(String text, int width, boolean rightAligned) {
			this.width = width;

			SpringLayout layout = new SpringLayout();
			setLayout(layout);

			JLabel label = new ThemedLabel(text);
			label.setFont(new Font("Source Sans Pro", Font.PLAIN, 21));
			add(label);

			layout.putConstraint(SpringLayout.VERTICAL_CENTER, label, 0, SpringLayout.VERTICAL_CENTER, this);
			if(rightAligned) {
				layout.putConstraint(SpringLayout.EAST, label, 0, SpringLayout.EAST, this);
			} else {
				layout.putConstraint(SpringLayout.WEST, label, 0, SpringLayout.WEST, this);
			}
		}

		public void setWidth(int width) {
			this.width = width;
			revalidate();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(width, getParent().getHeight());
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
		public Color getBackground() {
			if(getParent() != null) {
				return getParent().getBackground();
			}
			return Theme.selected.tracklistBackground;
		}
	}
}
