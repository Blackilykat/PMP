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

package dev.blackilykat.pmp.client.handlers;

import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.MessageHandler;
import dev.blackilykat.pmp.PMPConnection;
import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Filter;
import dev.blackilykat.pmp.client.Library;
import dev.blackilykat.pmp.client.Player;
import dev.blackilykat.pmp.client.Server;
import dev.blackilykat.pmp.messages.FilterListMessage;
import dev.blackilykat.pmp.messages.LoginSuccessResponse;
import dev.blackilykat.pmp.messages.PlaybackOwnershipMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.List;

public class LoginSuccessResponseHandler extends MessageHandler<LoginSuccessResponse> {
	private static final Logger LOGGER = LogManager.getLogger(LoginSuccessResponseHandler.class);

	public LoginSuccessResponseHandler() {
		super(LoginSuccessResponse.class);
	}

	@Override
	public void run(PMPConnection connection, LoginSuccessResponse message) {
		if(message.deviceId != null) {
			ClientStorage.SENSITIVE.deviceID.set(message.deviceId);
		}
		Server.deviceId = ClientStorage.SENSITIVE.deviceID.get();
		ClientStorage.SENSITIVE.token.set(message.token);

		// a nice side effect of this approach is that when the server first has empty filters, the first client to
		// connect will send its filters as lastKnownServerFilters is also empty by default
		if(!ClientStorage.MAIN.lastKnownServerFilters.get().equals(message.filters)) {
			Library.importFilters(message.filters);
		} else if(checkLocalFilterChanges(ClientStorage.MAIN.filters.get(), message.filters)) {
			connection.send(new FilterListMessage(Library.exportFilters()));
		}

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
				Player.load(ClientStorage.MAIN.tracks.get(message.track), false, false);
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

		Server.lastActionId = message.lastActionId;

		Server.EVENT_LOGGED_IN.call(null);
	}

	private static boolean checkLocalFilterChanges(List<Filter> local, List<FilterInfo> remote) {
		if(local.size() != remote.size()) {
			return true;
		}
		for(int i = 0; i < local.size(); i++) {
			Filter localFilter = local.get(i);
			FilterInfo remoteFilter = remote.get(i);
			if(localFilter.id != remoteFilter.id()) {
				return true;
			}
			if(!localFilter.key.equals(remoteFilter.key())) {
				return true;
			}
		}
		return false;
	}
}
