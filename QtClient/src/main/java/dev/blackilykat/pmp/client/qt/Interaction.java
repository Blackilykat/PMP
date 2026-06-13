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

package dev.blackilykat.pmp.client.qt;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Track;
import io.qt.core.QObject;

/// This class serves as a bridge between the UI and the logic.
/// JS snippets from QML call these methods which perform the requested action.
class Interaction extends QObject {
	public final static Logger LOGGER = LogManager.getLogger(Interaction.class);
	public void playPause() {
		Player.playPause();
	}

	public void shuffle() {
		Player.setShuffle(switch(Player.getShuffle()) {
			case ON -> ShuffleOption.OFF;
			case OFF -> ShuffleOption.ON;
		});
	}

	public void repeat() {
		Player.setRepeat(switch(Player.getRepeat()) {
			case OFF -> RepeatOption.ALL;
			case ALL -> RepeatOption.TRACK;
			case TRACK -> RepeatOption.OFF;
		});
	}


	public void previous() {
		Player.previous();
	}

	public void next() {
		Player.next();
	}

	public void seek(double percent) {
		percent = Math.clamp(percent, 0, 1);
		Player.seek((long) (Player.getTrack().getDurationSeconds() * 1000 * percent));
	}

	public void playTrack(String filename) {
		Track track = ClientStorage.MAIN.tracks.get(filename);
		if(track == null) {
			LOGGER.warn("QML tried to play nonexistent track {}", filename);
			return;
		}
		Player.play(track);
	}

	public void resizeHeader(int id, double delta) {
		for(var item : Tracklist.headerListModel.items) {
			if(item.id != id) continue;

			int newWidth = Math.max(0, item.width + (int) delta);

			item.setWidth(newWidth);
			QtStorage.MAIN.headerWidths.put(id, newWidth);

			break;
		}
	}
}
