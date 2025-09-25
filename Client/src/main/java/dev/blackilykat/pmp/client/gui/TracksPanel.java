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
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Track;
import dev.blackilykat.pmp.client.gui.util.ThemedLabel;
import dev.blackilykat.pmp.client.gui.util.ThemedVerticalScrollPane;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TracksPanel extends JPanel {
	private static final Logger LOGGER = LogManager.getLogger(TracksPanel.class);
	private static final int DOUBLE_CLICK_MS = 750;

	private List<Header> headers = new ArrayList<>();
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

		addHeader(1, "N°", "tracknumber", 50, true);
		addHeader(2, "Title", "title", 600, false);
		addHeader(3, "Artist", "artist", 600, false);
		addHeader(4, "Duration", "duration", 100, true);

		// TEMPORARY: deciding which tracks to display should be handled outside the gui.
		Library.EVENT_LOADED.register(tracks -> {
			SwingUtilities.invokeLater(() -> {
				for(Track track : tracks) {
					String tracknumber = "";
					StringBuilder artists = new StringBuilder();
					String duration = ((int) track.getDurationSeconds() / 60) + ":" + String.format("%02d",
							(int) track.getDurationSeconds() % 60);

					for(Pair<String, String> metadatum : track.metadata) {
						if(metadatum.key.equalsIgnoreCase("tracknumber")) {
							tracknumber = metadatum.value;
						}

						if(metadatum.key.equalsIgnoreCase("artist")) {
							if(artists.isEmpty()) {
								artists.append(metadatum.value);
							} else {
								artists.append(", ").append(metadatum.value);
							}
						}
					}

					contentPanel.add(
							new TrackPanel(track, tracknumber, track.getTitle(), artists.toString(), duration));
					contentPanel.revalidate();
				}
			});
		});
	}

	public void addHeader(int id, String label, String value, int width, boolean rightAligned) {
		headers.add(new Header(id, label, value, width, rightAligned));
		headersPanel.add(new HeaderPanel(label, width, rightAligned));
	}

	@Override
	public Color getBackground() {
		return Theme.selected.tracklistBackground;
	}

	public void removeHeader(int id) {
		Iterator<Header> it = headers.iterator();
		while(it.hasNext()) {
			if(it.next().id == id) {
				it.remove();
				break;
			}
		}
	}

	private class TrackPanel extends JPanel {
		private boolean playing = false;
		private boolean clicked = false;
		private Instant lastClick = Instant.EPOCH;

		public TrackPanel(Track track, String... strings) {
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			if(strings.length != headers.size()) {
				throw new IllegalArgumentException("Expected " + headers.size() + " values");
			}

			add(Box.createRigidArea(
					new Dimension(Theme.selected.trackPlayIconPadding * 2 + Theme.selected.trackPlayIconSize, 0)));
			for(int i = 0; i < headers.size(); i++) {
				Header h = headers.get(i);
				add(new TrackDataPanel(strings[i], h.width, h.rightAligned));
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
		}

		@Override
		public Color getBackground() {
			return Theme.selected.panelBackground;
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(getParent().getWidth(), 50);
		}

		public Component add(HeaderPanel panel) {
			Component c = super.add(panel);
			super.add(new HeaderPanel.PaddingPanel(panel));

			return c;
		}
	}

	private static class HeaderPanel extends JPanel {
		protected int width;
		private String text;

		public HeaderPanel(String text, int width, boolean rightAligned) {
			this.width = width;
			this.text = text;

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
			return Theme.selected.panelBackground;
		}

		public static class PaddingPanel extends JPanel {
			HeaderPanel hp;

			public PaddingPanel(HeaderPanel hp) {
				this.hp = hp;
				addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						LOGGER.info("Clicked on line for {}", hp.text);
					}
				});
			}

			@Override
			public Color getBackground() {
				return Theme.selected.panelBackground;
			}

			@Override
			public void paint(Graphics g) {
				g.setColor(Theme.selected.text);
				int width = getWidth();
				int height = getHeight();
				int sw = Theme.selected.headerSeparatorWidth;
				int svs = Theme.selected.headerSeparatorVerticalSpacing;

				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.fillRoundRect((width / 2) - sw, svs, sw, height - svs * 2, sw, sw);
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
		private String text;

		public TrackDataPanel(String text, int width, boolean rightAligned) {
			this.width = width;
			this.text = text;

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

	private static class Header {
		public int id;
		public String label;
		public String value;
		public int width;
		public boolean rightAligned;

		public Header(int id, String label, String value, int width, boolean rightAligned) {
			this.id = id;
			this.label = label;
			this.value = value;
			this.width = width;
			this.rightAligned = rightAligned;
		}
	}
}
