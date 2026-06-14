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
import QtQuick.Controls

// very silly way to do it but why is this not in qt quick
Button {
	id: btn
	property color color: Style.text
	property string source: ""

	palette {
		disabled.button: "#00000000"
	}

	enabled: false
	padding: 0

	icon {
		source: btn.source
		color: btn.color

		width: btn.width
		height: btn.height
	}
}

