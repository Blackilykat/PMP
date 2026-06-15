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
import QtQuick.Layouts
import QtQuick.Shapes
import QtQuick.Dialogs

Rectangle {
	id: tracklist
	color: Style.tracklistBackground

	property bool draggingHeader: false

	MouseArea {
		id: tracklistMA
		cursorShape: tracklist.draggingHeader ? Qt.DragMoveCursor : Qt.ArrowCursor
		anchors.fill: parent
	}

	property real playIconWidth: 40

	Rectangle {
		id: header
		color: Style.panelBackground
		height: 50
		z: 1
		anchors {
			top: parent.top
			left: parent.left
			right: parent.right
		}

		Row {
			Rectangle {
				width: tracklist.playIconWidth
				height: parent.height
				color: "#00000000"
			}

			Repeater {
				model: trackHeadersModel

				delegate: Row {
					Rectangle {
						width: headerWidth
						height: header.height
						color: headerMouseArea.dragging ? "#00000000" : headerMouseArea.pressed ? Style.clicked : headerMouseArea.containsMouse ? Style.hover : "#00000000"
						Text {
							elide: Text.ElideRight
							text: name
							color: Style.text
							font.pixelSize: 22
							horizontalAlignment: rightAligned ? Text.AlignRight : Text.AlignLeft
							topPadding: 10
							bottomPadding: 10
							anchors.fill: parent
							opacity: headerMouseArea.dragging ? 0.5 : 1
						}

						MouseArea {
							id: headerMouseArea
							anchors.fill: parent
							hoverEnabled: true

							cursorShape: tracklistMA.cursorShape

							property real dragStartX: 0
							property bool dragStarted: false
							property bool dragging: false
							property bool handleClick: false

							onClicked: {
								if(!dragging && handleClick) Interaction.sortByHeader(headerId);
							}

							onReleased: e => {
								if(dragging) {
									dragStarted = false
									dragging = false
									tracklist.draggingHeader = false
									e.accepted = true 
									handleClick = false
									return;
								}
							}

							onPressed: {
								handleClick = true
							}

							onPositionChanged: e => {
								if(e.buttons & Qt.LeftButton) {
									if(
										dragStarted &&
										!dragging && 
										(Math.abs(dragStartX - e.x) > 20 || e.x < 0 || e.x > headerWidth)
									) {
										dragging = true
										tracklist.draggingHeader = true
									}
									if(!dragStarted) {
										dragStartX = e.x;
										dragStarted = true;
									}

									if(e.x < 0) {
										if(index == 0) return;
										let delta = e.x + 20;
										let prevWidth = trackHeadersModel.get(index - 1).headerWidth

										if(-delta > prevWidth / 2) {
											Interaction.moveHeader(headerId, false);
										}
									} else if(e.x > headerWidth) {
										if(index == trackHeadersModel.rowCount() - 1) return;
										let delta = e.x - headerWidth - 20;
										let nextWidth = trackHeadersModel.get(index + 1).headerWidth

										if(delta > nextWidth / 2) {
											Interaction.moveHeader(headerId, true);
										}
									}
								}
							}
						}

						Shape {
							id: descendingTriangle
							width: 10
							height: 6
							visible: sorting == "DESCENDING"

							anchors {
								horizontalCenter: parent.horizontalCenter
								top: parent.top
								topMargin: 5
							}
							ShapePath {
								startX: descendingTriangle.width / 2
								startY: 0
								fillColor: Style.text
								strokeWidth: 0
								PathLine { x: descendingTriangle.width; y: descendingTriangle.height }
								PathLine { x: 0; y: descendingTriangle.height }
								PathLine { x: descendingTriangle.width / 2; y: 0 }
							}
						}

						Shape {
							id: ascendingTriangle
							width: 10
							height: 6
							visible: sorting == "ASCENDING"

							anchors {
								horizontalCenter: parent.horizontalCenter
								bottom: parent.bottom
								bottomMargin: 5
							}
							ShapePath {
								startX: descendingTriangle.width / 2
								startY: descendingTriangle.height
								fillColor: Style.text
								strokeWidth: 0
								PathLine { x: descendingTriangle.width; y: 0 }
								PathLine { x: 0; y: 0 }
								PathLine { x: descendingTriangle.width / 2; y: descendingTriangle.height }
							}
						}
					}
					Rectangle {
						height: parent.height
						width: 20
						color: mousearea.pressed ? Style.clicked : mousearea.containsMouse ? Style.hover : "#00000000"

						Rectangle {
							color: Style.text
							width: 2

							anchors {
								left: parent.left
								leftMargin: (parent.width / 2) - (width / 2)
								top: parent.top
								topMargin: 10
								bottom: parent.bottom
								bottomMargin: 10
							}
						}

						MouseArea {
							id: mousearea
							anchors.fill: parent
							hoverEnabled: true

							cursorShape: tracklistMA.cursorShape

							onPositionChanged: e => {
								if(pressed) {
									Interaction.resizeHeader(headerId, e.x - (width / 2));
								}
							}
						}
					}

				}
			}
		}
	}

	ScrollView {
		anchors {
			top: header.bottom
			left: parent.left
			right: parent.right
			bottom: parent.bottom
		}

		ScrollBar.vertical.policy: ScrollBar.AlwaysOn

		ListView {
			id: list
			z: 0

			width: parent.width

			model: tracklistModel

			delegate: Item {
				height: row.height
				width: list.width
				Row {
					id: row

					Item {
						width: tracklist.playIconWidth
						height: parent.height

						Icon {
							padding: 7
							visible: playing
							height: parent.height
							width: height * 0.6666
							source: "icons/play.svg"
						}
					}

					Repeater {
						id: thedata

						model: metadata

						delegate: Row {
							Text {
								id: rowtext
								elide: Text.ElideRight
								text: value
								// prevent binding the index which becomes -1 on removal
								width: {width = trackHeadersModel.get(index).headerWidth}
								color: Style.text
								font.pixelSize: 22
								horizontalAlignment: rightAligned ? Text.AlignRight : Text.AlignLeft
								topPadding: 10
								bottomPadding: 10

								// A binding would be better, but idk how to do that and I've spent enough hours on this already
								Connections {
									target: trackHeadersModel
									function onDataChanged() {
										rowtext.width = trackHeadersModel.get(index).headerWidth
									}
								}
							}
							Rectangle {
								color: "#00000000"
								width: 20
								height: parent.height
							}
						}
					}
				}

				Rectangle {
					color: trackMouseArea.pressed ? Style.clicked : trackMouseArea.containsMouse ? Style.hover : "#00000000"
					anchors.fill: parent
				}

				MouseArea {
					id: trackMouseArea
					anchors.fill: parent
					hoverEnabled: true

					cursorShape: tracklistMA.cursorShape

					onDoubleClicked: {
						Interaction.playTrack(filename)
					}
				}

				ContextMenu.menu: Menu {
					MenuItem {
						text: "Add tracks..."
						onTriggered: {
							Interaction.addTracks()
						}
					}
					MenuItem {
						text: "Remove " + title
						onTriggered: {
							Interaction.removeTrack(filename)
						}
					}
				}
			}
		}
	}
}
