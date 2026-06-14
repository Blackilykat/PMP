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


import QtQuick
// import QtQuick.Controls.Basic
import "."

Window {
	id: root

	color: Style.tracklistBackground

	visible: true
	title: "PMP"

	Tracklist {
		id: tracklist
		objectName: "tracklist"
		anchors {
			top: parent.top
			left: filters.right
			right: parent.right
			bottom: playbar.top
		}
	}

	Filters {
		id: filters
		objectName: "filters"
		anchors {
			top: parent.top
			left: parent.left
			bottom: playbar.top
		}
	}

	Playbar {
		id: playbar
		objectName: "playbar"
		anchors.bottom: parent.bottom
		anchors.left: parent.left
		anchors.right: parent.right
	}

	Shortcut {
		sequence: "Space"
		onActivated: Interaction.playPause()
	}

	Shortcut {
		sequence: "Left"
		onActivated: Interaction.seekBackward()
	}

	Shortcut {
		sequence: "Right"
		onActivated: Interaction.seekForward()
	}
}

