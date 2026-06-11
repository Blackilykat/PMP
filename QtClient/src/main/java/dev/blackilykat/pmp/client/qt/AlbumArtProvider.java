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
import io.qt.core.QSize;
import io.qt.gui.QImage;
import io.qt.quick.QQuickImageProvider;

/// Image provider that always returns the currently playing track's album art.
///
/// Works because currently the only album art displayed is the current track's.
///
/// If needed, this can be expanded to properly handle the id which is the requested track's filename.
class AlbumArtProvider extends QQuickImageProvider {
	private byte[] data = null;

	public AlbumArtProvider() {
		super(ImageType.Image);

		Player.EVENT_TRACK_CHANGE.register(event -> {
			event.picture().thenAccept(data -> {
				this.data = data;
			});
		});
	}

	@Override
	public QImage requestImage(String id, QSize size, QSize requestedSize){
		return QImage.fromData(data);
	}

}
