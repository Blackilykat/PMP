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
import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Server;
import dev.blackilykat.pmp.messages.LoginSuccessResponse;
import dev.blackilykat.pmp.messages.PlaybackOwnershipMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;

import java.time.Instant;

public class LoginSuccessResponseHandler extends MessageHandler<LoginSuccessResponse> {
	public LoginSuccessResponseHandler() {
		super(LoginSuccessResponse.class);
	}

	@Override
	public void run(PMPConnection connection, LoginSuccessResponse message) {
		ClientStorage cs = ClientStorage.getInstance();
		if(message.deviceId != null) {
			cs.setDeviceID(message.deviceId);
		}
		Server.deviceId = cs.getDeviceID();
		cs.setToken(message.token);

		if(!Player.getPaused() && message.playbackOwner == null) {
			Player.setPlaybackOwner(message.deviceId);
			connection.send(new PlaybackOwnershipMessage());
			PlaybackUpdateMessage update = new PlaybackUpdateMessage();
			update.playing = !Player.getPaused();
			update.negativeOptions = Library.exportNegativeOptions();
			update.positiveOptions = Library.exportPositiveOptions();
			update.track = Player.getTrack().getFile().getName();
			update.positionOrEpoch = Player.getPlaybackEpoch();
			update.repeat = Player.getRepeat();
			update.shuffle = Player.getShuffle();
			connection.send(update);
		} else {
			ScopedValue.where(Player.DONT_SEND_UPDATES, true).run(() -> {
				Player.setPlaybackOwner(message.playbackOwner);
				Player.load(Library.getTrackByFilename(message.track), false, false);
				if(message.playing) {
					Player.seek(Instant.now().toEpochMilli() - message.positionOrEpoch);
					Player.play();
				} else {
					Player.pause();
					Player.seek(message.positionOrEpoch);
				}
				if(message.shuffle != null) {
					Player.setShuffle(message.shuffle);
				}
				if(message.repeat != null) {
					Player.setRepeat(message.repeat);
				}
				Library.importFilterOptions(message.positiveOptions, message.negativeOptions);
			});
		}
	}
}
