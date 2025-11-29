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
import dev.blackilykat.pmp.messages.PlaybackControlMessage;

import java.util.Objects;

public class PlaybackControlMessageHandler extends MessageHandler<PlaybackControlMessage> {
	public PlaybackControlMessageHandler() {
		super(PlaybackControlMessage.class);
	}

	@Override
	public void run(PMPConnection connection, PlaybackControlMessage message) {
		if(!Objects.equals(Player.getPlaybackOwner(), Server.deviceId)) {
			Server.send(new ErrorMessage("I'm not the playback owner"));
			return;
		}
		if(message.negativeOptions != null && message.positiveOptions != null) {
			Library.importFilterOptions(message.positiveOptions, message.negativeOptions);
		}

		boolean playing = !Player.getPaused();
		if(message.playing != null) {
			playing = message.playing;
		}

		if(message.position != null) {
			if(message.playing == null) {
				Player.seek(message.position);
			} else {
				// position is sent over along with playing
				ScopedValue.where(Player.DONT_SEND_UPDATES, true).run(() -> {
					Player.seek(message.position);
				});
			}
		}
		if(message.track != null) {
			Player.pause();
			Player.load(Library.getTrackByFilename(message.track), true, playing);
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
	}
}
