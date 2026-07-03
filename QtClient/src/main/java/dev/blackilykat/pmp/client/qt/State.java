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

import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Server;
import dev.blackilykat.pmp.client.audio.AudioBackend;
import io.qt.core.QMetaObject;
import io.qt.core.QObject;
import io.qt.qml.QQmlContext;

class State extends QObject {
	public static State instance = null;

	public final QBooleanProperty connected = new QBooleanProperty(false);

	// never updated, should never change during execution
	public final QProperty<String> audioBackend = new QProperty<>(AudioBackend.backend.getClass().getSimpleName());

	public final QIntProperty debugLatency = new QIntProperty(0);
	public final QIntProperty debugFramePosition = new QIntProperty(0);
	public final QIntProperty debugExpectedPos = new QIntProperty(0);
	public final QIntProperty debugTrackLength = new QIntProperty(0);
	public final QIntProperty debugBufferSize = new QIntProperty(0);
	public final QDoubleProperty debugExpectedWriteTime = new QDoubleProperty(0.0);
	public final QDoubleProperty debugTotalOffset = new QDoubleProperty(0.0);

	public static void initialize(QQmlContext context) {
		instance = new State();

		context.setContextProperty("globalState", instance);

		Server.EVENT_CONNECTED.register(_ -> {
			QMetaObject.invokeMethod(instance, () -> {
				instance.connected.setValue(true);
			});
		});

		Server.EVENT_DISCONNECTED.register(_ -> {
			QMetaObject.invokeMethod(instance, () -> {
				instance.connected.setValue(false);
			});
		});

		instance.connected.setValue(Server.isConnected());

		Player.EVENT_PLAYBACK_DEBUG_INFO.register(e -> {
			QMetaObject.invokeMethod(instance, () -> {
				instance.debugLatency.setValue(e.latency());
				instance.debugFramePosition.setValue(e.framePosition());
				instance.debugExpectedPos.setValue(e.expectedPos());
				instance.debugTrackLength.setValue(e.trackLength());
				instance.debugBufferSize.setValue(e.bufferSize());
				instance.debugExpectedWriteTime.setValue(e.expectedWriteTime());
				instance.debugTotalOffset.setValue(e.totalOffset());
			});
		});
	}
}
