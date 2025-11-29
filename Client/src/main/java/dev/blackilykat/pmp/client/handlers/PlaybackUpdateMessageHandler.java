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

package dev.blackilykat.pmp.client.handlers;

import dev.blackilykat.pmp.MessageHandler;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Server;
import dev.blackilykat.pmp.messages.ErrorMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;

import java.time.Instant;
import java.util.Objects;

public class PlaybackUpdateMessageHandler extends MessageHandler<PlaybackUpdateMessage> {
	public PlaybackUpdateMessageHandler() {
		super(PlaybackUpdateMessage.class);
	}

	@Override
	public void run(PMPConnection connection, PlaybackUpdateMessage message) {
		if(Objects.equals(Player.getPlaybackOwner(), Server.deviceId)) {
			connection.send(
					new ErrorMessage("I'm the playback owner, I shouldn't be receiving PlaybackUpdateMessages"));
			return;
		}

		ScopedValue.where(Player.DONT_SEND_UPDATES, true).run(() -> {

			if(message.negativeOptions != null && message.positiveOptions != null) {
				Library.importFilterOptions(message.positiveOptions, message.negativeOptions);
			}

			boolean playing = !Player.getPaused();
			if(message.playing != null) {
				playing = message.playing;
			}

			if(message.positionOrEpoch != null) {
				if(playing) {
					if(Player.getPaused()) {
						Player.seek(Instant.now().toEpochMilli() - message.positionOrEpoch);
					} else {
						Player.setPlaybackEpoch(message.positionOrEpoch);
					}
				} else {
					if(Player.getPaused()) {
						Player.seek(message.positionOrEpoch);
					} else {
						Player.setPlaybackEpoch(Instant.now().toEpochMilli() - message.positionOrEpoch);
					}
				}
			}
			if(message.track != null) {
				Player.load(Library.getTrackByFilename(message.track), false, playing);
			} else if(Boolean.TRUE.equals(message.playing)) {
				Player.play();
			} else if(Boolean.FALSE.equals(message.playing)) {
				Player.pause();
			}

			if(message.repeat != null) {
				Player.setRepeat(message.repeat);
			}
			if(message.shuffle != null) {
				Player.setShuffle(message.shuffle);
			}
		});
	}
}
