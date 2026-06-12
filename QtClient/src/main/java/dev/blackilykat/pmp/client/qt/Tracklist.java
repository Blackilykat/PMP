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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Header;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Track;
import io.qt.NonNull;
import io.qt.core.QByteArray;
import io.qt.core.QList;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QVariant;
import io.qt.gui.QGuiApplication;
import io.qt.qml.QQmlContext;

class Tracklist {
	// stolen from SwingClient
	public static final Map<String, String> COMMON_METADATA_KEYS_LABELS = Map.of("album", "Album", "albumartist",
			"Album Artist", "artist", "Artist", "date", "Date", "duration", "Duration", "title", "Title",
			"tracknumber",
			"N°");
	public static final Map<Header.Type, Integer> INITIAL_HEADER_WIDTHS = Map.of(Header.Type.TRACKNUMBER, 50,
			Header.Type.INTEGER, 50, Header.Type.DOUBLE, 70, Header.Type.DURATION, 100, Header.Type.TITLE, 400,
			Header.Type.STRING, 300);

	public static final List<Header.Type> RIGHT_ALIGNED_TYPES = List.of(Header.Type.DOUBLE, Header.Type.INTEGER,
			Header.Type.DURATION, Header.Type.TRACKNUMBER);

	public static void initialize(QQmlContext context) {

		TrackListModel trackListModel = new TrackListModel(QGuiApplication.instance());
		context.setContextProperty("tracklistModel", new QVariant(trackListModel));
		context.setContextProperty("trackHeadersModel", new QVariant(new TrackHeaderListModel(QGuiApplication.instance())));
		QList<Integer> headerWidths = new QList<>(Integer.class);
		ClientStorage.MAIN.headers.get().forEach(header -> {
			headerWidths.add(Tracklist.INITIAL_HEADER_WIDTHS.get(header.type));
		});

		context.setContextProperty("headerWidths", headerWidths);

		trackListModel.replace(Library.getSelectedTracks());
		Library.EVENT_SELECTED_TRACKS_UPDATED.register(event -> {
			trackListModel.replace(event.newSelection());
		});
	}

	private static class TrackListModel extends ReplaceableListModel<Track> {
		public static final int ROLE_FILENAME = 0x0101;
		public static final int ROLE_METADATA = 0x0102;
		public static final int ROLE_PLAYING = 0x0103;

		private Map<Track, TrackMetadataListModel> metadata = new HashMap<>();

		public TrackListModel(QObject parent) {
			super(parent);

			Library.EVENT_HEADERS_UPDATED.register(event -> {
				metadata.values().forEach(TrackMetadataListModel::update);
			});

			Player.EVENT_TRACK_CHANGE.register(event -> {
				dataChanged.emit(index(0, 0), index(items.size() - 1, 0));
			});

		}

		@Override
		public Object data(@NonNull QModelIndex arg0, int arg1) {
			var row = items.get(arg0.row());
			return switch(arg1) {
				case ROLE_FILENAME -> new QVariant(row.getFile().getName());
				case ROLE_METADATA -> {
					if(metadata.containsKey(row)) {
						yield new QVariant(metadata.get(row));
					}

					var dis = new TrackMetadataListModel(row, parent());
					metadata.put(row, dis);
					yield new QVariant(dis);
				}
				case ROLE_PLAYING -> row == Player.getTrack();
				default -> new QVariant();
			};
		}

		@Override
		public Map<Integer, QByteArray> roleNames() {
			return Map.of(
				ROLE_FILENAME, new QByteArray("filename"),
				ROLE_METADATA, new QByteArray("metadata"),
				ROLE_PLAYING, new QByteArray("playing")
			);
		}
	}

	private static class TrackHeaderListModel extends ReplaceableListModel<TrackHeaderObject> {
		public static final int ROLE_HEADER = 0x0101;

		public TrackHeaderListModel(QObject parent) {
			super(parent);

			replace(ClientStorage.MAIN.headers.get().stream().map(header -> {
				return new TrackHeaderObject(header.getLabel(), Tracklist.RIGHT_ALIGNED_TYPES.contains(header.type));
			}).toList());
		}

		@Override
		public Object data(@NonNull QModelIndex arg0, int arg1) {
			return new QVariant(items.get(arg0.row()));
		}

		@Override
		public Map<Integer, QByteArray> roleNames() {
			return Map.of(ROLE_HEADER, new QByteArray("header"));
		}
	}

	public static class TrackMetadataListModel extends ReplaceableListModel<TrackMetadataObject> {
		public static final int ROLE_VALUE = 0x0101;
		public static final int ROLE_RIGHT_ALIGNED = 0x0102;
		public final Track track;

		public TrackMetadataListModel(Track track, QObject parent) {
			super(parent);
			this.track = track;
			update();
		}

		public void update() {
			replace(ClientStorage.MAIN.headers.get().stream().map(header -> {
				return new TrackMetadataObject(
					header.getStringValue(track),
					Tracklist.RIGHT_ALIGNED_TYPES.contains(header.type)
				);
			}).toList());
		}

		@Override
		public Object data(@NonNull QModelIndex arg0, int arg1) {
			var row = items.get(arg0.row());
			return switch(arg1) {
				case ROLE_VALUE -> new QVariant(row.value);
				case ROLE_RIGHT_ALIGNED -> new QVariant(row.rightAligned);
				default -> new QVariant();
			};
		}

		@Override
		public Map<Integer, QByteArray> roleNames() {
			return Map.of(
				ROLE_VALUE, new QByteArray("value"),
				ROLE_RIGHT_ALIGNED, new QByteArray("rightAligned")
			);
		}
	}

	public static class TrackMetadataObject extends QObject {
		public final QBooleanProperty playing = new QBooleanProperty(false);
		public String value;
		public boolean rightAligned;

		public TrackMetadataObject(String value, boolean rightAligned) {
			this.value = value;
			this.rightAligned = rightAligned;
			super();
		}
	}


	public static class TrackHeaderObject extends QObject {
		public String name;
		public boolean rightAligned;

		public TrackHeaderObject(String name, boolean rightAligned) {
			this.name = name;
			this.rightAligned = rightAligned;
		}
	}
}
