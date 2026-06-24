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

package dev.blackilykat.pmp;

import java.io.File;

/// Class containing various global values which may be overridden during
/// startup to change the behavior of underlying shared code.
public class Globals {
	/// The directory where all storage is saved to and loaded from.
	public static File dataRoot = new File(".");
	/// The directory where tracks are saved to and loaded from.
	public static File library = new File(dataRoot, "library");
}
