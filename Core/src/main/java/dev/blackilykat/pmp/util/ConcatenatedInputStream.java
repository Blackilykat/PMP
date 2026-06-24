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

package dev.blackilykat.pmp.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

/// Similar to [SequenceInputStream], but the available method reports the sum of available bytes from both streams.
public class ConcatenatedInputStream extends InputStream {
	protected InputStream a;
	protected InputStream b;
	protected boolean aClosed = false;

	/// Construct this ConcatenatedInputStream using the two other given InputStreams.
	public ConcatenatedInputStream(InputStream a, InputStream b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public int read() throws IOException {
		if(aClosed) {
			return b.read();
		}
		int res = a.read();
		if(res == -1) {
			aClosed = true;
			return b.read();
		}
		return res;
	}

	@Override
	public int available() throws IOException {
		return a.available() + b.available();
	}
}
