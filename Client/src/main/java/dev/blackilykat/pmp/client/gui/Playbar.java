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
import com.github.weisj.jsvg.view.ViewBox;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Track;
import dev.blackilykat.pmp.client.gui.util.ThemedLabel;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Playbar extends JPanel {
	private static final Logger LOGGER = LogManager.getLogger(Playbar.class);

	public JLabel title = new ThemedLabel("Vancouver");
	public JLabel artists = new ThemedLabel("Artist1, Artist2");
	public JLabel currentTime = new ThemedLabel("1:41");
	public JLabel duration = new ThemedLabel("3:42");
	public TimeSlider track = new TimeSlider();
	public AlbumArtDisplay albumArt = new AlbumArtDisplay();
	public RoundButton shuffleButton = new RoundButton(45, Theme.selected.shuffleOnIcon);
	public RoundButton previousButton = new RoundButton(45, Theme.selected.previousIcon);
	public RoundButton playPauseButton = new RoundButton(60, Theme.selected.pauseIcon);
	public RoundButton nextButton = new RoundButton(45, Theme.selected.nextIcon);
	public RoundButton repeatButton = new RoundButton(45, Theme.selected.repeatAllIcon);

	public Playbar() {
		SpringLayout layout = new SpringLayout();
		setLayout(layout);

		add(title);
		add(artists);
		add(currentTime);
		add(duration);
		add(track);
		add(albumArt);
		add(shuffleButton);
		add(previousButton);
		add(playPauseButton);
		playPauseButton.addActionListener(e -> {
			Player.playPause();
		});
		add(nextButton);
		add(repeatButton);

		Player.EVENT_PROGRESS.register(ms -> {
			SwingUtilities.invokeLater(() -> {
				Track currentTrack = Player.getTrack();
				if(currentTrack != null) {
					double percentage = ms / (currentTrack.getDurationSeconds() * 1000);
					track.progressValue((int) (percentage * 100_000));
				}

				currentTime.setText(secondsToString((int) (ms / 1000)));
			});
		});

		Player.EVENT_TRACK_CHANGE.register(event -> {
			Track track = event.track();
			CompletableFuture<byte[]> albumArtFuture = event.picture();
			SwingUtilities.invokeLater(() -> {
				title.setText(track.getTitle());
				List<String> artistList = new LinkedList<>();
				for(Pair<String, String> metadatum : track.metadata) {
					if(metadatum.key.equalsIgnoreCase("artist")) {
						artistList.add(metadatum.value);
					}
				}
				artists.setText(String.join(", ", artistList));

				duration.setText(secondsToString((int) track.getDurationSeconds()));

				albumArt.setImage(null);
				albumArtFuture.thenAccept(data -> {
					if(data == null) {
						return;
					}
					try {
						ByteArrayInputStream is = new ByteArrayInputStream(data);
						Image img = ImageIO.read(is);
						albumArt.setImage(img);
					} catch(IOException e) {
						LOGGER.error("Unexpected IOException", e);
					}
				});
			});
		});

		Player.EVENT_PLAY_PAUSE.register(paused -> {
			SwingUtilities.invokeLater(() -> {
				if(paused) {
					playPauseButton.setIcon(Theme.selected.playIcon);
				} else {
					playPauseButton.setIcon(Theme.selected.pauseIcon);
				}
			});
		});

		Player.EVENT_CURRENT_TRACK_LOAD.register(event -> {
			int loaded = event.loaded();
			int total = event.total();

			track.setLoadedPercentage((double) loaded / total);
		});

		layout.putConstraint(SpringLayout.NORTH, playPauseButton, 20, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, playPauseButton, 0, SpringLayout.HORIZONTAL_CENTER, this);

		layout.putConstraint(SpringLayout.EAST, previousButton, -10, SpringLayout.WEST, playPauseButton);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, previousButton, 0, SpringLayout.VERTICAL_CENTER,
				playPauseButton);

		layout.putConstraint(SpringLayout.EAST, shuffleButton, -10, SpringLayout.WEST, previousButton);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, shuffleButton, 0, SpringLayout.VERTICAL_CENTER,
				playPauseButton);

		layout.putConstraint(SpringLayout.WEST, nextButton, 10, SpringLayout.EAST, playPauseButton);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, nextButton, 0, SpringLayout.VERTICAL_CENTER,
				playPauseButton);

		layout.putConstraint(SpringLayout.WEST, repeatButton, 10, SpringLayout.EAST, nextButton);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, repeatButton, 0, SpringLayout.VERTICAL_CENTER,
				playPauseButton);


		layout.putConstraint(SpringLayout.WEST, albumArt, 20, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, albumArt, 20, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.SOUTH, albumArt, -20, SpringLayout.SOUTH, this);

		layout.putConstraint(SpringLayout.WEST, title, 0, SpringLayout.EAST, albumArt);
		layout.putConstraint(SpringLayout.NORTH, title, -5, SpringLayout.NORTH, albumArt);

		title.setFont(new Font("Source Sans Pro", Font.PLAIN, 32));

		layout.putConstraint(SpringLayout.WEST, artists, 0, SpringLayout.WEST, title);
		layout.putConstraint(SpringLayout.NORTH, artists, 0, SpringLayout.SOUTH, title);

		artists.setFont(new Font("Source Sans Pro", Font.PLAIN, 22));

		layout.putConstraint(SpringLayout.WEST, currentTime, 0, SpringLayout.EAST, albumArt);
		layout.putConstraint(SpringLayout.SOUTH, currentTime, -10, SpringLayout.SOUTH, this);

		currentTime.setFont(new Font("Source Sans Pro", Font.PLAIN, 24));

		layout.putConstraint(SpringLayout.EAST, duration, -20, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, duration, 0, SpringLayout.SOUTH, currentTime);

		duration.setFont(new Font("Source Sans Pro", Font.PLAIN, 24));

		layout.putConstraint(SpringLayout.WEST, track, 10, SpringLayout.EAST, currentTime);
		layout.putConstraint(SpringLayout.EAST, track, -10, SpringLayout.WEST, duration);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, track, 0, SpringLayout.VERTICAL_CENTER, currentTime);
	}


	@Override
	public Color getForeground() {
		return Theme.selected.text;
	}

	@Override
	public Color getBackground() {
		return Theme.selected.panelBackground;
	}

	private static String secondsToString(int seconds) {
		int minutes = seconds / 60;
		seconds %= 60;
		return String.format("%d:%02d", minutes, seconds);
	}

	public static class AlbumArtDisplay extends JPanel {
		private Image image = null;
		private Image oldImage = null;
		private Image renderedImage = null;
		private int oldHeight = -1;

		public void setImage(Image image) {
			this.image = image;
			reloadAntiAliasing();

			revalidate();
			repaint();
		}

		@Override
		public void paint(Graphics g) {
			if(renderedImage != null) {
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g.setClip(new RoundRectangle2D.Double(0, 0, getHeight(), getHeight(), Theme.selected.albumArtRadius,
						Theme.selected.albumArtRadius));
				g.drawImage(renderedImage, 0, 0, getHeight(), getHeight(), this);
			}
		}

		@Override
		public Dimension getPreferredSize() {
			if(image == null) {
				return new Dimension(0, 0);
			}
			// padding added here so when it's empty there's no padding
			return new Dimension(getHeight() + 10, getHeight());
		}

		@Override
		public void setBounds(int x, int y, int width, int height) {
			super.setBounds(x, y, width, height);
			reloadAntiAliasing();
		}

		private void reloadAntiAliasing() {
			int height = getHeight();
			if(image == null || height <= 0) {
				renderedImage = null;
				return;
			}
			if(image == oldImage && height == oldHeight) {
				return;
			}
			//noinspection SuspiciousNameCombination
			renderedImage = image.getScaledInstance(height, height, Image.SCALE_SMOOTH);
			oldImage = image;
			oldHeight = height;
		}
	}

	public static class TimeSlider extends JSlider {
		public static final int MAX_VALUE = 100_000;
		private boolean dragging = false;

		private double loadedPercentage = 1.0;

		public TimeSlider() {
			super(0, MAX_VALUE, 45_000);
		}

		@Override
		public Color getBackground() {
			if(getParent() == null) {
				return Theme.selected.panelBackground;
			}
			return getParent().getBackground();
		}

		public void setLoadedPercentage(double value) {
			loadedPercentage = value;
			repaint();
		}

		@Override
		public void updateUI() {
			setUI(new TimeSliderUI(this));
		}

		public void progressValue(int v) {
			if(dragging) {
				return;
			}
			setValue(v);
		}

		public void userValue(int v) {
			Track track = Player.getTrack();
			if(track == null) {
				return;
			}

			long ms = (long) (v / (double) MAX_VALUE * track.getDurationSeconds() * 1000);

			LOGGER.info("User sought to {}% ({}ms)", v / 1000d, ms);

			Player.seek(ms);
		}

		public class TimeSliderUI extends BasicSliderUI {
			public TimeSliderUI(TimeSlider b) {
				super(b);
			}

			@Override
			protected TrackListener createTrackListener(JSlider slider) {
				return new TrackListener() {

					@Override
					public void mouseReleased(MouseEvent e) {
						updateValue(e);
						dragging = false;
						userValue(getValue());
					}

					@Override
					public void mousePressed(MouseEvent e) {
						if(isEnabled()) {
							dragging = true;
							updateValue(e);
						}
					}

					@Override
					public void mouseDragged(MouseEvent e) {
						if(isEnabled() && dragging) {
							updateValue(e);
						}
					}

					private void updateValue(MouseEvent e) {
						int v = Math.clamp(e.getX() - trackRect.x, 0, trackRect.width);

						double percentage = ((double) v) / trackRect.width;

						setValue((int) (percentage * getMaximum()));
					}
				};
			}

			@Override
			protected Dimension getThumbSize() {
				return new Dimension(10, 40);
			}

			@Override
			public void paintFocus(Graphics g) {
			}

			@Override
			public void paintTrack(Graphics g) {
				int loadedPx = (int) (trackRect.width * loadedPercentage);
				g.setColor(Theme.selected.text);
				g.fillRect(trackRect.x, trackRect.y + trackRect.height / 2 - 2, loadedPx, 4);

				g.setColor(Theme.selected.playbarLoading);
				g.fillRect(trackRect.x + loadedPx, trackRect.y + trackRect.height / 2 - 2, trackRect.width - loadedPx,
						4);
			}

			@Override
			public void paintThumb(Graphics g) {
				g.setColor(Theme.selected.text);
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, thumbRect.width,
						thumbRect.width);
			}
		}
	}

	public static class RoundButton extends JButton {
		protected int diameter;
		protected SVGDocument icon;
		private boolean clicked = false;

		public RoundButton(int diameter, SVGDocument icon) {
			this.diameter = diameter;
			this.icon = icon;
			setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			setBackground(Theme.selected.buttonBackground);
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
		}

		public void setIcon(SVGDocument icon) {
			this.icon = icon;
			repaint();
		}

		@Override
		public void paint(Graphics g) {
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g.setColor(getParent().getBackground());
			g.fillRect(0, 0, diameter, diameter);

			g.setColor(Theme.selected.buttonBackground);
			g.fillOval(0, 0, diameter, diameter);

			Point mouse = getMousePosition();
			if(clicked) {
				g.setColor(Theme.selected.clicked);
				g.fillOval(0, 0, diameter, diameter);
			} else if(mouse != null && contains(mouse)) {
				g.setColor(Theme.selected.hover);
				g.fillOval(0, 0, diameter, diameter);
			}

			g.setColor(Theme.selected.text);
			icon.render(this, (Graphics2D) g, new ViewBox((getWidth() - Theme.selected.buttonIconSize) / 2f,
					(getHeight() - Theme.selected.buttonIconSize) / 2f, Theme.selected.buttonIconSize,
					Theme.selected.buttonIconSize));
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(diameter, diameter);
		}

		@Override
		public boolean contains(int x, int y) {
			double radius = diameter / 2.0;
			double cx = x - radius;
			double cy = y - radius;

			return radius * radius >= cx * cx + cy * cy;
		}
	}
}
