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

Rectangle {
	id: timebar
	color: "#00000000"

	property double progress: 0

	property real mouseX: 0

	Rectangle {
		id: track
		height: 4
		color: Style.text
		border.width: 0

		anchors {
			verticalCenter: parent.verticalCenter
			left: parent.left
			right: parent.right
		}
	}

	Rectangle {
		id: thumb
		width: 9
		color: Style.text
		radius: 10

		anchors {
			left: parent.left
			top: parent.top
			bottom: parent.bottom
			leftMargin: (mousearea.pressed ? timebar.mouseX : timebar.progress * timebar.width) - (width / 2)
		}
	}

	MouseArea {
		id: mousearea
		anchors.fill: parent

		onPositionChanged: {
			if(mouse.x < 0) {
				timebar.mouseX = 0
			} else if(mouse.x > width) {
				timebar.mouseX = width
			} else {
				timebar.mouseX = mouse.x
			}
		}

		onPressed: {
			timebar.mouseX = mouse.x
		}

		onReleased: {
			Interaction.seek(mouse.x / width)
		}
	}
}
