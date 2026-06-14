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


import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.blackilykat.pmp.Order;
import dev.blackilykat.pmp.RepeatOption;
import dev.blackilykat.pmp.ShuffleOption;
import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.FilterOption;
import dev.blackilykat.pmp.client.Header;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Track;
import io.qt.core.QObject;
import io.qt.core.QStandardPaths;
import io.qt.core.QStandardPaths.StandardLocation;
import io.qt.widgets.QFileDialog;

/// This class serves as a bridge between the UI and the logic.
/// JS snippets from QML call these methods which perform the requested action.
class Interaction extends QObject {
	public final static Logger LOGGER = LogManager.getLogger(Interaction.class);

	/// Executor used to **respond to** input without blocking the UI thread.
	/// Qt dialogs are not thread safe. Do not use this to ask for user input.
	public final static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();


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

	public void sortByHeader(int id) {
		Header current = Library.getSortingHeader();
		if(current.id == id) {
			Library.setSorting(current, switch(Library.getSortingOrder()) {
				case ASCENDING -> Order.DESCENDING;
				case DESCENDING -> Order.ASCENDING;
			});
			return;
		}


		for(var header : ClientStorage.MAIN.headers.get()) {
			if(header.id != id) continue;

			Library.setSorting(header, Order.ASCENDING);

			return;
		}

		LOGGER.warn("QML tried to sort by nonexistent header {}", id);
	}

	public void filterOption(int filterId, String option, boolean positive) {
		for(var filter : ClientStorage.MAIN.filters.get()) {
			if(filter.id != filterId) continue;

			for(var o : filter.getOptions()) {
				if(!o.value.equals(option)) continue;

				if(positive) {
					o.setState(switch(o.getState()) {
						case NONE -> FilterOption.State.POSITIVE;
						case POSITIVE -> FilterOption.State.NONE;
						case NEGATIVE -> FilterOption.State.POSITIVE;
					});
				} else {
					o.setState(switch(o.getState()) {
						case NONE -> FilterOption.State.NEGATIVE;
						case POSITIVE -> FilterOption.State.NEGATIVE;
						case NEGATIVE -> FilterOption.State.NONE;
					});
				}

				return;
			}

			LOGGER.warn("QML tried to interact with nonexistent filter option {} -> {}", filter.key, option);
			return;
		}
		LOGGER.warn("QML tried to interact with nonexistent filter {}", filterId);
	}

	public void addTracks() {
		// QML fallback dialog does not properly implement multiple file selection.
		// This keeps the context menu on the screen while a file is selected. Not ideal,
		// but better than not having multiple file selection.
		var res = QFileDialog.getOpenFileNames(
			null,
			"Add tracks",
			QStandardPaths.writableLocation(StandardLocation.MusicLocation),
			"FLAC files (*.flac)"
		);

		if(res == null) return;

		EXECUTOR.submit(() -> {

			for(String path : res.result) {
				File file = new File(path);
				try {
					Library.addTrack(file);
				} catch(FileAlreadyExistsException e) {
					LOGGER.warn("{} is already in the library", file);
				} catch(IOException e) {
					LOGGER.error("Failed to add track {}", path, e);
				}
			}
		});
	}

	public void removeTrack(String filename) {
		Track track = ClientStorage.MAIN.tracks.get(filename);
		if(track == null) {
			LOGGER.warn("QML tried to remove nonexistent track {}", filename);
			return;
		}
		Library.removeTrack(track);
	}
}
