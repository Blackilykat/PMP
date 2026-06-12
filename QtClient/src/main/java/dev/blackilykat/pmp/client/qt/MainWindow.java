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

import io.qt.NonNull;
import io.qt.core.QByteArray;
import io.qt.core.QCoreApplication;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QStringList;
import io.qt.core.QStringListModel;
import io.qt.core.QUrl;
import io.qt.core.QVariant;
import io.qt.gui.QGuiApplication;
import io.qt.qml.QQmlApplicationEngine;
import io.qt.qml.QQmlContext;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Main;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Server;
import dev.blackilykat.pmp.client.Track;
import dev.blackilykat.pmp.util.Pair;
import dev.blackilykat.pmp.util.Shutdown;

public class MainWindow {
	public final static Logger LOGGER = LogManager.getLogger(MainWindow.class);

	public MainWindow() {
	}

	static void main(String[] args) {
		QGuiApplication.initialize(args);

		QQmlApplicationEngine engine = new QQmlApplicationEngine();

		engine.addImageProvider("albumArt", new AlbumArtProvider());

		engine.rootContext().setContextProperty("Interaction", new Interaction());

		engine.addImportPath(QCoreApplication.applicationDirPath() + "/qml");
		engine.addImportPath("qrc:/ui");
		engine.load(new QUrl("qrc:/ui/App.qml"));

		QObject root = engine.rootObjects().getFirst();

		registerPropertyUpdates(root, engine.rootContext()); 

		Main.main(args);

		Server.EVENT_SHOULD_ASK_PASSWORD.register(_ -> {
			Server.submitPassword("mypassword");
		});

		QGuiApplication.exec();

		engine.dispose();
		engine = null;

		QGuiApplication.shutdown();

		Shutdown.shutdown(true);
	}

	private static void registerPropertyUpdates(QObject root, QQmlContext context) {
		QObject playbar = root.findChild("playbar");

		if(playbar == null) {
			LOGGER.warn("Could not find playbar QML object");
		} else {
			Player.EVENT_TRACK_CHANGE.register(event -> {
				if(event.track() == null) return;
				playbar.setProperty("title", event.track().getTitle());
				playbar.setProperty("artists", event.track().getArtists().stream().collect(Collectors.joining(", ")));

				playbar.setProperty("length", event.track().getDurationSeconds());

				event.picture().thenAccept(data -> {
					if(data == null) {
						playbar.setProperty("albumArt", "");
					} else {
						playbar.setProperty("albumArt", "image://albumArt/" + event.track().getFile().getName());
					}
				});
			});
			Player.EVENT_PROGRESS.register(milliseconds -> {
				playbar.setProperty("position", milliseconds / 1000.0);
			});
			Player.EVENT_PLAY_PAUSE.register(paused -> {
				playbar.setProperty("playing", !paused);
			});
			Player.EVENT_SHUFFLE_CHANGED.register(shuffle -> {
				playbar.setProperty("shuffle", shuffle.toString());
			});
			Player.EVENT_REPEAT_CHANGED.register(repeat -> {
				playbar.setProperty("repeat", repeat.toString());
			});
		}

		TrackListModel trackListModel = new TrackListModel(QGuiApplication.instance());
		context.setContextProperty("tracklistModel", new QVariant(trackListModel));

		Library.EVENT_SELECTED_TRACKS_UPDATED.register(event -> {
			trackListModel.replace(event.newSelection());
		});

	}

	private static class TrackListModel extends ReplaceableListModel<Track> {
		public static final int ROLE_TITLE = 0x0101;
		public static final int ROLE_METADATA = 0x0102;
		
		private Map<Track, TrackMetadataListModel> metadata = new HashMap<>();

		public TrackListModel(QObject parent) {
			super(parent);

			Library.EVENT_HEADERS_UPDATED.register(event -> {
				metadata.values().forEach(TrackMetadataListModel::update);
			});
		}

		@Override
		public Map<Integer, QByteArray> roleNames() {
			return Map.of(
				ROLE_TITLE, new QByteArray("title"),
				ROLE_METADATA, new QByteArray("metadata")
			);
		}

		@Override
		public Object data(@NonNull QModelIndex arg0, int arg1) {
			var row = items.get(arg0.row());
			return switch(arg1) {
				case ROLE_TITLE -> new QVariant(row.getTitle());
				case ROLE_METADATA -> {
					if(metadata.containsKey(row)) yield new QVariant(metadata.get(row));

					var dis = new TrackMetadataListModel(row, parent());
					metadata.put(row, dis);
					yield new QVariant(dis);
				}
				default -> new QVariant();
			};
		}
	}

	public static class TrackMetadataListModel extends ReplaceableListModel<TrackMetadataObject> {
		public static final int ROLE_VALUE = 0x0101;
		public final Track track;

		public TrackMetadataListModel(Track track, QObject parent) {
			super(parent);
			this.track = track;
			update();
		}

		public void update() {
			replace(ClientStorage.MAIN.headers.get().stream().map(header -> {
				return new TrackMetadataObject(header.getStringValue(track), false);
			}).toList());
		}

		@Override
		public Map<Integer, QByteArray> roleNames() {
			return Map.of(
				ROLE_VALUE, new QByteArray("value")
			);
		}

		@Override
		public Object data(@NonNull QModelIndex arg0, int arg1) {
			return items.get(arg0.row()).value;
		}
	}

	public static class TrackMetadataObject extends QObject {
		public String value;
		public boolean rightAligned;

		public TrackMetadataObject(String value, boolean rightAligned) {
			this.value = value;
			this.rightAligned = rightAligned;
		}

	}

}
