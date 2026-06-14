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
import Qt5Compat.GraphicalEffects
import "."

Rectangle {
	id: playbar
	height: 140
	color: Style.panelBackground

	property string title: ""
	property string artists: ""
	property double position: 0
	property double length: 0
	property bool playing: false
	property string albumArt: ""
	property string shuffle: "OFF"
	property string repeat: "ALL"

	palette {
		button: Style.buttonBackground
		buttonText: Style.text
	}


	PlaybarButton {
		id: playPauseButton
		width: 60
		height: 60

		anchors {
			top: parent.top
			topMargin: 20
			horizontalCenter: parent.horizontalCenter
		}

		icon {
			width: 16
			height: 24
			source: playbar.playing ? "icons/pause.svg" : "icons/play.svg"
		}

		onClicked: Interaction.playPause()
	}

	PlaybarButton {
		id: previousButton
		width: 50
		height: 50

		anchors {
			verticalCenter: playPauseButton.verticalCenter
			right: playPauseButton.left
			rightMargin: 10
		}

		icon {
			width: 16
			height: 24
			source: "icons/previous.svg"
		}

		onClicked: Interaction.previous()
	}

	PlaybarButton {
		id: shuffleButton
		width: 50
		height: 50

		anchors {
			verticalCenter: previousButton.verticalCenter
			right: previousButton.left
			rightMargin: 10
		}

		icon {
			width: 24
			height: 24
			source: `icons/shuffle-${ playbar.shuffle.toLowerCase() }.svg`
		}

		onClicked: Interaction.shuffle()
	}

	PlaybarButton {
		id: nextButton
		width: 50
		height: 50

		anchors {
			verticalCenter: playPauseButton.verticalCenter
			left: playPauseButton.right
			leftMargin: 10
		}

		icon {
			width: 16
			height: 24
			source: "icons/next.svg"
		}

		onClicked: Interaction.next()
	}

	PlaybarButton {
		id: repeatButton
		width: 50
		height: 50

		anchors {
			verticalCenter: nextButton.verticalCenter
			left: nextButton.right
			leftMargin: 10
		}


		icon {
			width: 24
			height: 24
			source: `icons/repeat-${ playbar.repeat.toLowerCase() }.svg`
		}

		onClicked: Interaction.repeat()
	}

	Image {
		id: albumArt
		width: playbar.albumArt == "" ? 0 : height

		source: playbar.albumArt
		fillMode: Image.PreserveAspectCrop

		// seemingly a good enough compromise between smoothness and contrast.
		// both set to true result in very blurry images.
		// The scaling on the swing client is really good. Might be worth replicating
		// that since the image data is served through java code anyway
		mipmap: true
		smooth: false

		anchors {
			left: parent.left
			top: parent.top
			bottom: parent.bottom
			leftMargin: 20
			topMargin: 20
			bottomMargin: 20
		}

		layer {
			enabled: true
			effect: OpacityMask {
				maskSource: Item {
					width: albumArt.width
					height: albumArt.height
					Rectangle {
						anchors.centerIn: parent
						width: albumArt.width
						height: albumArt.height
						radius: 10
					}
				}
			}
		}
	}

	Text {
		id: titleText
		color: Style.text
		text: playbar.title
		elide: Text.ElideRight
		font.pixelSize: 30
		font.family: Style.font

		anchors {
			left: albumArt.right
			top: parent.top
			leftMargin: 10
			topMargin: 20
			right: shuffleButton.left
			rightMargin: 0
		}
	}

	Text {
		id: artists
		color: Style.text
		text: playbar.artists
		elide: Text.ElideRight
		font.pixelSize: 20
		font.family: Style.font

		anchors {
			left: titleText.left
			top: titleText.bottom
			leftMargin: 0
			topMargin: 5
			right: shuffleButton.left
			rightMargin: 0
		}
	}

	Text {
		id: currentTime
		color: Style.text
		text: secToStr(playbar.position)
		font.pixelSize: 24
		font.family: Style.font

		anchors {
			left: titleText.left
			bottom: parent.bottom
			leftMargin: 0
			bottomMargin: 10
		}
	}

	Text {
		id: totalTime
		color: Style.text
		text: secToStr(playbar.length)
		font.pixelSize: 24
		font.family: Style.font

		anchors {
			right: parent.right
			bottom: parent.bottom
			rightMargin: 10
			bottomMargin: 10
		}
	}

	TimeBar {
		id: timeBar
		height: 36

		progress: playbar.position / playbar.length

		anchors {
			verticalCenter: currentTime.verticalCenter
			left: currentTime.right
			right: totalTime.left
			leftMargin: 16
			rightMargin: 16
		}
	}

	// TODO: move this
	function secToStr(seconds) {
		let minutes = seconds / 60;
		seconds %= 60;
		return `${Math.floor(minutes)}:${`${Math.floor(seconds)}`.padStart(2, "0")}`
	}
}
