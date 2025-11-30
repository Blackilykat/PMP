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
import dev.blackilykat.pmp.messages.FilterListMessage;

public class FilterListMessageHandler extends MessageHandler<FilterListMessage> {
	public FilterListMessageHandler() {
		super(FilterListMessage.class);
	}

	@Override
	public void run(PMPConnection connection, FilterListMessage message) {
		Library.importFilters(message.filters);
		ClientStorage.getInstance().setLastKnownServerFilters(message.filters);
	}
}
