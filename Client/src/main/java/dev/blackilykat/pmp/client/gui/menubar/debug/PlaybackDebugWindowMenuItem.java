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

package dev.blackilykat.pmp.client.gui.menubar.debug;

import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.gui.Theme;
import dev.blackilykat.pmp.client.gui.util.ThemedLabel;
import dev.blackilykat.pmp.event.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PlaybackDebugWindowMenuItem extends JMenuItem {
	private static final Logger LOGGER = LogManager.getLogger(PlaybackDebugWindowMenuItem.class);

	public PlaybackDebugWindowMenuItem() {
		super("Open playback debug window");

		addActionListener(e -> {
			JFrame window = new JFrame("Playback debug");
			Container content = window.getContentPane();
			BoxLayout layout = new BoxLayout(content, BoxLayout.PAGE_AXIS);
			content.setLayout(layout);
			ThemedLabel pulse = new ThemedLabel("Pulse: ?");
			pulse.setAlignmentX(0);
			content.add(pulse);
			ThemedLabel framePosition = new ThemedLabel("Frame position: ?");
			framePosition.setAlignmentX(0);
			content.add(framePosition);
			ThemedLabel latency = new ThemedLabel("Latency: ?");
			latency.setAlignmentX(0);
			content.add(latency);
			ThemedLabel expectedPos = new ThemedLabel("Expected pos: ?");
			expectedPos.setAlignmentX(0);
			content.add(expectedPos);
			ThemedLabel bufferSize = new ThemedLabel("Buffer size: ?");
			bufferSize.setAlignmentX(0);
			content.add(bufferSize);
			ThemedLabel trackLength = new ThemedLabel("Track length: ?");
			trackLength.setAlignmentX(0);
			content.add(trackLength);
			ThemedLabel expectedWriteTime = new ThemedLabel("Expected write time: ?");
			expectedWriteTime.setAlignmentX(0);
			content.add(expectedWriteTime);
			ThemedLabel totalOffset = new ThemedLabel("Offset: ?");
			totalOffset.setAlignmentX(0);
			content.add(totalOffset);
			PositionPanel positionPanel = new PositionPanel();
			positionPanel.setAlignmentX(0);
			content.add(positionPanel);

			JButton printButton = new JButton("Print");
			printButton.addActionListener(ae -> {
				LOGGER.debug("Logging playback debug info");
				LOGGER.debug(pulse.getText());
				LOGGER.debug(framePosition.getText());
				LOGGER.debug(latency.getText());
				LOGGER.debug(expectedPos.getText());
				LOGGER.debug(bufferSize.getText());
				LOGGER.debug(trackLength.getText());
				LOGGER.debug(expectedWriteTime.getText());
				LOGGER.debug(totalOffset.getText());
			});
			content.add(printButton);

			Listener<Player.PlaybackDebugInfoEvent> listener = event -> {
				SwingUtilities.invokeLater(() -> {
					pulse.setText("Pulse: " + event.pulse());
					framePosition.setText("Frame position: " + event.framePosition());
					latency.setText("Latency: " + event.latency());
					expectedPos.setText("Expected pos: " + event.expectedPos());
					bufferSize.setText("Buffer size: " + event.bufferSize());
					trackLength.setText("Track length: " + event.trackLength());
					positionPanel.update(event.totalOffset());
					expectedWriteTime.setText("Expected write time: " + event.expectedWriteTime() + "ms");
					totalOffset.setText("Offset: " + event.totalOffset() + "ms");
				});
			};

			Player.EVENT_PLAYBACK_DEBUG_INFO.register(listener);

			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					Player.EVENT_PLAYBACK_DEBUG_INFO.unregister(listener);
				}
			});

			window.setVisible(true);
		});
	}

	private static class PositionPanel extends JPanel {
		private final static int MAX = 1000;
		private double offset = 0;

		public PositionPanel() {
		}

		public void update(double offset) {
			this.offset = offset;
			repaint();
		}

		@Override
		public void paint(Graphics g) {
			if(offset < 0) {
				g.setColor(Color.RED);
			} else {
				g.setColor(Theme.selected.buttonBackground);
			}
			g.fillRect(0, 0, getWidth(), getHeight());

			double offsetPercent = offset / MAX;

			g.setColor(Theme.selected.text);
			g.fillRect((int) (getWidth() * offsetPercent), 0, 1, getHeight());
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(500, 30);
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
