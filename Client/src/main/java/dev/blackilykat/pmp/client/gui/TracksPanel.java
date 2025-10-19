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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
		updateTracks();

		Library.EVENT_HEADER_ADDED.register(header -> {
			GUIUtils.runOnSwingThread(() -> {
				addHeader(header);
				updateTracks();
			});
		});

		Library.EVENT_HEADER_REMOVED.register(header -> {
			GUIUtils.runOnSwingThread(() -> {
				removeHeader(header);
			});
		});

		Library.EVENT_HEADER_MOVED.register(event -> {
			Header header = event.header();
			int newPosition = event.newPosition();

			GUIUtils.runOnSwingThread(() -> {
				moveHeader(header, newPosition);
				updateTracks();
			});
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
		Map<Integer, Integer> headerWidths = SwingStorage.getInstance().getHeaderWidths();
		if(!headerWidths.containsKey(header.id)) {
			Integer width = INITIAL_HEADER_WIDTHS.get(header.type);
			if(width == null) {
				width = 400;
				LOGGER.error("TracksPanel#addHeader: Unexpected null initial width for header type {}, "
						+ "falling back to {}, this should be unreachable", header.type, width);
			}
			headerWidths.put(header.id, width);
			SwingStorage.getInstance().setHeaderWidths(headerWidths);
		}

		HeaderPanel hp = new HeaderPanel(headersPanel, this, header, headersPanel.getCount());
		if(Library.getSortingHeader() == hp.header) {
			hp.setSortingOrder(Library.getSortingOrder());
		}

		header.eventLabelChanged.register(label -> {
			hp.setLabel(label);
			updateTracks();
		});

		headersPanel.add(hp);

		headersPanel.revalidate();
		headersPanel.repaint();
	}

	private void moveHeader(Header header, int newPosition) {
		Component[] componentsBeforeMoving = headersPanel.getComponents();

		boolean foundFirstHP = false;
		int offset = 0;

		HeaderPanel headerPanel = null;
		HeaderPanel.PaddingPanel paddingPanel = null;

		for(Component component : componentsBeforeMoving) {
			if(component instanceof HeaderPanel hp) {
				foundFirstHP = true;
				if(hp.header == header) {
					headerPanel = hp;
				}
			} else if(component instanceof HeaderPanel.PaddingPanel pp) {
				if(pp.headerPanel.header == header) {
					paddingPanel = pp;
				}
			} else {
				if(!foundFirstHP) {
					offset++;
				} else {
					LOGGER.warn("Unexpected HeadersPanel structure, this should be unreachable "
							+ "(TracksPanel#moveHeader)");
				}
			}
		}

		headersPanel.remove((Component) headerPanel);
		headersPanel.remove(paddingPanel);

		headersPanel.add(headerPanel, newPosition * 2 + offset);
		headersPanel.add(paddingPanel, newPosition * 2 + 1 + offset);

		int n = 0;
		for(Component component : headersPanel.getComponents()) {
			if(component instanceof HeaderPanel hp) {
				hp.index = n;
				n++;
			}
		}

		headersPanel.revalidate();
		headersPanel.repaint();
	}

	private void removeHeader(Header header) {
		for(Component component : headersPanel.getComponents()) {
			if(component instanceof HeaderPanel hp && hp.header == header) {
				headersPanel.remove(hp);
				break;
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
		GUIUtils.runOnSwingThread(() -> {
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


	private static JMenuItem buildEditHeaderItem(Header header) {
		JMenuItem item = new JMenuItem("Edit " + header.getLabel() + "...");
		item.addActionListener(e -> {

			AtomicReference<String> key = new AtomicReference<>(header.getKey());
			AtomicReference<String> label = new AtomicReference<>(header.getLabel());

			if(!promptEditHeaderValues("Edit a header", key, label)) {
				return;
			}

			LOGGER.info("Edited Header#{}: label {} -> {}, key {} -> {}", header.id, header.getLabel(), label.get(),
					header.getKey(), key.get());

			header.setKey(key.get());
			header.setLabel(label.get());
		});
		return item;
	}

	private static JMenuItem buildAddHeaderItem() {
		JMenuItem item = new JMenuItem("Add header...");
		item.addActionListener(e -> {

			AtomicReference<String> key = new AtomicReference<>();
			AtomicReference<String> label = new AtomicReference<>();

			if(!promptEditHeaderValues("Add a new header", key, label)) {
				return;
			}

			Header header = new Header(ClientStorage.getInstance().getAndIncrementCurrentHeaderId(), label.get(),
					key.get());


			Library.addHeader(header);
		});
		return item;
	}

	/**
	 * @return false if the user has cancelled the prompt (closed the window or pressed cancel)
	 */
	private static boolean promptEditHeaderValues(String title, AtomicReference<String> key,
			AtomicReference<String> label) {
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

		if(key.get() != null) {
			keyField.setSelectedItem(key.get());
		}

		if(label.get() != null) {
			labelField.setText(label.get());
		}

		if(key.get() != null && label.get() != null && !headerLabelFromKey(key.get()).equals(label.get())) {
			customLabelCheckbox.setSelected(true);
		}

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
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, labelLabel, 0, SpringLayout.VERTICAL_CENTER, labelField);

		panel.add(keyLabel);
		panel.add(keyField);
		panel.add(customLabelCheckbox);
		panel.add(labelLabel);
		panel.add(labelField);

		panel.setPreferredSize(new Dimension(300, 230));
		int chosen = JOptionPane.showOptionDialog(null, panel, title, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, null, null);

		if(chosen == JOptionPane.CANCEL_OPTION || chosen == JOptionPane.CLOSED_OPTION) {
			return false;
		}

		key.set(((JTextComponent) keyField.getEditor().getEditorComponent()).getText());
		label.set(labelField.getText());

		return true;
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
				label = headerLabelFromKey(selected);
			}
			labelField.setText(label);
		}
	}

	private static String headerLabelFromKey(String key) {
		StringBuilder builder = new StringBuilder();
		boolean capitalize = true;
		for(char c : key.toCharArray()) {
			String s = Character.toString(c);


			if(capitalize) {
				builder.append(s.toUpperCase());
			} else {
				builder.append(s.toLowerCase());
			}

			capitalize = c == ' ';
		}
		return builder.toString();
	}

	private static JMenuItem buildRemoveHeaderItem(Header header) {
		JMenuItem item = new JMenuItem("Remove " + header.getLabel());
		item.addActionListener(e -> {
			Library.removeHeader(header);
		});
		return item;
	}

	private static JMenuItem buildAddTrackItem() {
		JMenuItem item = new JMenuItem("Add track...");
		item.addActionListener(e -> {

			JFileChooser fileChooser = new JFileChooser();

			fileChooser.setMultiSelectionEnabled(true);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

			fileChooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".flac");
				}

				@Override
				public String getDescription() {
					return "Folders and FLAC files";
				}
			});

			if(fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
				return;
			}

			List<File> files = new LinkedList<>();
			for(File file : fileChooser.getSelectedFiles()) {
				files.addAll(crawlSubdirectories(file));
			}

			for(File file : files) {
				try {
					if(Library.addTrack(file)) {
						LOGGER.info("Added track {}", file);
					} else {
						LOGGER.info("Skipped track {} as it doesn't seem to be a valid FLAC file", file);
					}
				} catch(FileAlreadyExistsException ex) {
					LOGGER.info("Skipped track {} as it already exists", file);
				} catch(IOException ex) {
					LOGGER.error("Failed to add track {}", file, ex);
				}
			}
		});

		return item;
	}

	private static List<File> crawlSubdirectories(File file) {
		if(file == null || !file.exists()) {
			throw new IllegalArgumentException("Does not exist");
		}
		if(!file.isDirectory()) {
			return List.of(file);
		}

		List<File> list = new LinkedList<>();
		crawlSubdirectories(file, list);
		return list;
	}

	private static void crawlSubdirectories(File directory, List<File> list) {
		if(!directory.isDirectory()) {
			throw new IllegalArgumentException("Not a directory");
		}
		File[] files = directory.listFiles();
		if(files == null) {
			return;
		}
		for(File file : files) {
			if(file.isDirectory()) {
				crawlSubdirectories(file, list);
			} else {
				list.add(file);
			}
		}
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


			Map<Integer, Integer> headerWidths = SwingStorage.getInstance().getHeaderWidths();

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

			JPopupMenu menu = new JPopupMenu();
			menu.add(buildAddTrackItem());
			addMouseListener(GUIUtils.createPopupListener(menu, this));
		}

		public void updateWidths() {
			Map<Integer, Integer> headerWidths = SwingStorage.getInstance().getHeaderWidths();

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
		public HeaderPanel movingHeader = null;
		private int count = 0;

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
			count++;
		}

		public void remove(HeaderPanel panel) {
			Component[] components = getComponents();
			int i = 0;
			while(components[i] != panel) {
				i++;
			}

			remove(i + 1);
			remove(i);

			count--;
		}

		public int getCount() {
			return count;
		}
	}

	private static class HeaderPanel extends JPanel {
		public final TracksPanel tp;
		public final HeadersPanel headersPanel;
		public final Header header;
		public int index;
		protected int width;
		private Order sortingOrder = null;
		private boolean clicked = false;
		private boolean hovered = false;
		private JLabel label;
		private int draggingStartX = -1;

		public HeaderPanel(HeadersPanel headersPanel, TracksPanel tp, Header header, int index) {
			this.tp = tp;
			this.headersPanel = headersPanel;
			Map<Integer, Integer> headerWidths = SwingStorage.getInstance().getHeaderWidths();
			this.width = headerWidths.get(header.id);
			this.header = header;
			this.index = index;

			super();

			SpringLayout layout = new SpringLayout();
			setLayout(layout);

			label = new JLabel(header.getLabel()) {
				@Override
				public Color getForeground() {
					if(headersPanel.movingHeader == HeaderPanel.this) {
						return Theme.selected.disabledText;
					} else {
						return Theme.selected.text;
					}
				}
			};
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
			menu.add(buildEditHeaderItem(header));
			menu.add(buildRemoveHeaderItem(header));

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) {
						clicked = true;
						repaint();
						draggingStartX = e.getXOnScreen();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) {
						clicked = false;
						draggingStartX = -1;
						if(headersPanel.movingHeader == HeaderPanel.this) {
							headersPanel.movingHeader = null;
							tp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						} else {
							if(sortingOrder == Order.ASCENDING) {
								Library.setSorting(header, Order.DESCENDING);
							} else {
								Library.setSorting(header, Order.ASCENDING);
							}
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

			addMouseMotionListener(new MouseAdapter() {
				// Sometimes an event queues up with coordinates relative to the position of the component before
				// moving it elsewhere, which makes it "overshoot" the movement. This ignores that queued up event
				private boolean ignoreNext = false;

				@Override
				public void mouseDragged(MouseEvent e) {
					// must be BUTTON1
					if(!clicked) {
						return;
					}
					if(headersPanel.movingHeader != HeaderPanel.this
							&& Math.abs(e.getXOnScreen() - draggingStartX) > 20) {
						headersPanel.movingHeader = HeaderPanel.this;
						tp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
						repaint();
					}

					if(ignoreNext) {
						ignoreNext = false;
						return;
					}
					int pad = Theme.selected.headerPadding * 2;
					if(headersPanel.movingHeader == HeaderPanel.this) {
						int i = HeaderPanel.this.index;
						List<Header> h = Library.getHeaders();

						Header prev = i == 0 ? null : h.get(i - 1);
						Header next = i == h.size() - 1 ? null : h.get(i + 1);

						if(prev != null && e.getX() < -pad - headerWidths.get(prev.id) / 2) {
							Library.moveHeader(header, HeaderPanel.this.index - 1);
							ignoreNext = true;
						} else if(next != null && e.getX() > width + pad + headerWidths.get(next.id) / 2) {
							Library.moveHeader(header, HeaderPanel.this.index + 1);
							ignoreNext = true;
						}
					}
				}
			});

			addMouseListener(GUIUtils.createPopupListener(menu, this));
		}

		public void setLabel(String label) {
			this.label.setText(label);
			repaint();
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
			if(headersPanel.movingHeader != null) {
				return base;
			}
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

				Map<Integer, Integer> headerWidths = SwingStorage.getInstance().getHeaderWidths();

				headerWidths.put(headerPanel.header.id, headerWidths.get(headerPanel.header.id) + offset);
				headerPanel.width = headerWidths.get(headerPanel.header.id);
				headerPanel.revalidate();

				SwingStorage.getInstance().setHeaderWidths(headerWidths);

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

			JPopupMenu menu = new JPopupMenu();
			menu.add(buildAddTrackItem());
			addMouseListener(GUIUtils.createPopupListener(menu, this));
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
