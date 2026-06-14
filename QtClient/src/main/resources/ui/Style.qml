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


pragma Singleton
import QtQuick

QtObject {
	property color panelBackground: "#343434"
	property color text: "#b8b8b8"
	property color disabledText: "#909090"
	property color tracklistBackground: "#252525"

	property color buttonBackground: "#151515"
	property color disabledButtonBackground: "#202020"
	property color filterPanelBackground: "#1d1d1d"

	property color filterOptionBackground: "#262626"
	property color filterOptionBackgroundPositive: "#24432A"
	property color filterOptionBackgroundNegative: "#4E1C1C"

	property color playbarLoading: "#707070"

	property color menuBarBackground: "#2f2f2f"

	property color scrollBarColor: "#66FFFFFF"
	property color hover: "#0CFFFFFF"
	property color clicked: "#1AFFFFFF"

	property string font: "DejaVu Sans"
}
