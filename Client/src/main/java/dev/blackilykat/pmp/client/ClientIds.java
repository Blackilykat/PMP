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

package dev.blackilykat.pmp.client;

import dev.blackilykat.pmp.FilterInfo;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;

public final class ClientIds {
	private ClientIds() {}

	public static int randomFilterId() {
		return randomUnusedId(id -> {
			for(Filter filter : ClientStorage.MAIN.filters.get()) {
				if(filter.id == id) {
					return true;
				}
			}

			for(FilterInfo filterInfo : ClientStorage.MAIN.lastKnownServerFilters.get()) {
				if(filterInfo.id() == id) {
					return true;
				}
			}

			return false;
		});
	}

	public static int randomHeaderId() {
		return randomUnusedId(id -> {
			for(Header header : ClientStorage.MAIN.headers.get()) {
				if(header.id == id) {
					return true;
				}
			}

			return false;
		});
	}

	private static int randomUnusedId(IntPredicate isUsed) {
		int id;
		do {
			id = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
		} while(isUsed.test(id));
		return id;
	}
}
