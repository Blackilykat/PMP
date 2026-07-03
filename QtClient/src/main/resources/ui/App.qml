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
import QtQuick.Controls.Fusion
import "."

ApplicationWindow {
	id: root

	color: Style.tracklistBackground

	visible: true
	title: "PMP"

	menuBar: MenuBar {
		Menu {
			title: "&Connection"
			Action {
				text: "&Connect"
				enabled: !globalState.connected
				onTriggered: Interaction.connectServer();
			}
			Action {
				text: "&Disconnect"
				enabled: globalState.connected
				onTriggered: Interaction.disconnectServer();
			}
		}
		Menu {
			title: "&Debug"
			Action {
				text: "Enter Debug &Log..."
				onTriggered: debugLogDialog.show();
			}
			Action {
				text: "Open &Playback Debug Window"
				onTriggered: playbackDebugWindow.visible = true;
			}
		}
	}

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

	ApplicationWindow {
		id: debugLogDialog
		visible: false
		title: "Enter a debug log"

		height: inputs.height + buttons.height + 50
		maximumHeight: height
		minimumHeight: height
		width: inputs.width + 100
		maximumWidth: width
		minimumWidth: width

		function show() {
			messageInput.text = "Something happened";

			visible = true;
		}

		function submit() {
			// send the message to the Java side to log through Log4j
			// JS logs go straight to stdout without passing through LoggingProxy
			Interaction.debugLog(messageInput.text)

			debugLogDialog.visible = false;
		}

		Column {
			id: inputs
			anchors {
				top: parent.top
				topMargin: 10
				left: parent.left
				leftMargin: 10
			}

			Row {
				Label { text: "Message: " }
				TextField {
					id: messageInput
					width: 500
					onAccepted: debugLogDialog.submit()
				}
			}
		}

		Row {
			id: buttons
			anchors {
				bottom: parent.bottom
				bottomMargin: 10
				right: parent.right
				rightMargin: 10
			}

			Button {
				text: "Ok"
				onClicked: debugLogDialog.submit()
			}

			Button {
				text: "Cancel"
				onClicked: {
					debugLogDialog.visible = false;
				}
			}
		}
	}

	ApplicationWindow {
		id: playbackDebugWindow
		visible: false

		Column {
			Label { text: `Backend: ${globalState.audioBackend}` }
			Label { text: `Frame position: ${globalState.debugFramePosition}` }
			Label { text: `Latency: ${globalState.debugLatency}` }
			Label { text: `Expected pos: ${globalState.debugExpectedPos}` }
			Label { text: `Buffer size: ${globalState.debugBufferSize}` }
			Label { text: `Track length: ${globalState.debugTrackLength}` }
			Label { text: `Expected write time: ${globalState.debugExpectedWriteTime}ms` }
			Label { text: `Offset: ${globalState.debugTotalOffset}ms` }

			Rectangle {
				height: 20
				width: 400
				color: globalState.debugTotalOffset < 0 ? "red" : "black"

				Rectangle {
					height: parent.height
					width: 1
					color: "white"
					anchors {
						top: parent.top
						left: parent.left
						leftMargin: (globalState.debugTotalOffset / 1000) * parent.width
					}
				}
			}
		}
	}
}

