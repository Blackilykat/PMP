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

import io.qt.core.QCoreApplication;
import io.qt.core.QObject;
import io.qt.core.QUrl;
import io.qt.qml.QQmlApplicationEngine;
import io.qt.widgets.QApplication;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.blackilykat.pmp.client.Main;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Server;
import dev.blackilykat.pmp.util.Shutdown;

public class MainWindow {
	public final static Logger LOGGER = LogManager.getLogger(MainWindow.class);

	public MainWindow() {
	}

	static void main(String[] args) {
		QApplication.initialize(args);

		QQmlApplicationEngine engine = new QQmlApplicationEngine();

		engine.addImageProvider("albumArt", new AlbumArtProvider());

		engine.rootContext().setContextProperty("Interaction", new Interaction());

		engine.addImportPath(QCoreApplication.applicationDirPath() + "/qml");
		engine.addImportPath("qrc:/ui");
		engine.load(new QUrl("qrc:/ui/App.qml"));

		QObject root = engine.rootObjects().getFirst();

		registerPropertyUpdates(root); 

		Main.main(args);

		Server.EVENT_SHOULD_ASK_PASSWORD.register(_ -> {
		});

		QApplication.exec();

		engine.dispose();
		engine = null;

		QApplication.shutdown();

		Shutdown.shutdown(true);
	}

	private static void registerPropertyUpdates(QObject root) {
		QObject playbar = root.findChild("playbar");

		if(playbar == null) {
			LOGGER.warn("Could not find playbar QML object");
		} else {
			Player.EVENT_TRACK_CHANGE.register(event -> {
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
	}

	
}
