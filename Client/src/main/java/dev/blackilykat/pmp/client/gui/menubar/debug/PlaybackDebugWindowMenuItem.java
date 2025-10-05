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

import javax.swing.BoxLayout;
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
	public PlaybackDebugWindowMenuItem() {
		super("Open playback debug window");

		addActionListener(e -> {
			// boolean pulse, int framePosition, int latency, int maxOffset, int bottomExpectedPos, int
			// topExpectedPos, int bufferSize, int trackLength

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
			ThemedLabel maxOffset = new ThemedLabel("Max offset: ?");
			maxOffset.setAlignmentX(0);
			content.add(maxOffset);
			ThemedLabel bottomExpectedPos = new ThemedLabel("Bottom expected pos: ?");
			bottomExpectedPos.setAlignmentX(0);
			content.add(bottomExpectedPos);
			ThemedLabel topExpectedPos = new ThemedLabel("Top expected pos: ?");
			topExpectedPos.setAlignmentX(0);
			content.add(topExpectedPos);
			ThemedLabel bufferSize = new ThemedLabel("Buffer size: ?");
			bufferSize.setAlignmentX(0);
			content.add(bufferSize);
			ThemedLabel trackLength = new ThemedLabel("Track length: ?");
			trackLength.setAlignmentX(0);
			content.add(trackLength);
			PositionPanel positionPanel = new PositionPanel();
			positionPanel.setAlignmentX(0);
			content.add(positionPanel);

			Listener<Player.PlaybackDebugInfoEvent> listener = event -> {
				SwingUtilities.invokeLater(() -> {
					pulse.setText("Pulse: " + event.pulse());
					framePosition.setText("Frame position: " + event.framePosition());
					latency.setText("Latency: " + event.latency());
					maxOffset.setText("Max offset: " + event.maxOffset());
					bottomExpectedPos.setText("Bottom expected pos: " + event.bottomExpectedPos());
					topExpectedPos.setText("Top expected pos: " + event.topExpectedPos());
					bufferSize.setText("Buffer size: " + event.bufferSize());
					trackLength.setText("Track length: " + event.trackLength());
					positionPanel.update(event.framePosition(), event.bottomExpectedPos(), event.topExpectedPos(),
							event.maxOffset());
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
		private int pos = 0;
		private int bottom = 0;
		private int top = 0;
		private int maxOffset = 0;

		public PositionPanel() {
		}

		public void update(int pos, int bottom, int top, int maxOffset) {
			this.pos = pos;
			this.bottom = bottom;
			this.top = top;
			this.maxOffset = maxOffset;
			repaint();
		}

		@Override
		public void paint(Graphics g) {
			int lowest = bottom - maxOffset;
			int highest = top + maxOffset;
			if(pos < lowest || pos > highest) {
				g.setColor(Color.RED);
			} else {
				g.setColor(Theme.selected.buttonBackground);
			}
			g.fillRect(0, 0, getWidth(), getHeight());

			highest -= lowest;
			int pos = this.pos - lowest;
			int bottom = this.bottom - lowest;
			int top = this.top - lowest;

			double posPercent = (double) pos / highest;
			double bottomPercent = (double) bottom / highest;
			double topPercent = (double) top / highest;

			g.setColor(Color.RED);
			g.fillRect((int) (getWidth() * bottomPercent), 0, 1, getHeight());
			g.setColor(Color.GREEN);
			g.fillRect((int) (getWidth() * topPercent), 0, 1, getHeight());
			g.setColor(Color.BLUE);
			g.fillRect((int) (getWidth() * posPercent), 0, 1, getHeight());
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
