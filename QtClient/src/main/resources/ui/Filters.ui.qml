import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Rectangle {
	id: filters
	color: Style.panelBackground

	width: 400

	ColumnLayout {
		anchors {
			top: parent.top
			bottom: parent.bottom
			left: parent.left
			right: parent.right
			topMargin: 15 // Align the the first filter's label with track headers
		}

		Repeater {
			model: filterListModel
			delegate: Rectangle {
				id: filter
				width: filters.width
				color: Style.panelBackground
				Layout.fillHeight: true

				Rectangle {
					id: label
					width: keytext.width + 16
					height: keytext.height + 8
					color: Style.filterPanelBackground
					topRightRadius: 10
					topLeftRadius: 10

					anchors {
						top: parent.top
						topMargin: 3
						left: parent.left
						leftMargin: 5
					}

					Text {
						id: keytext
						anchors {
							top: parent.top
							topMargin: 8
							left: parent.left
							leftMargin: 8
						}
						text: key
						font.pixelSize: 20
						font.family: Style.font
						color: Style.text

						font.capitalization: Font.Capitalize
					}
				}

				Rectangle {
					color: Style.filterPanelBackground
					anchors {
						top: label.bottom
						left: parent.left
						right: parent.right
						bottom: parent.bottom
					}

					ScrollView {
						clip: true
						anchors {
							top: parent.top
							right: parent.right
							bottom: parent.bottom
							left: parent.left
							topMargin: 5
							bottomMargin: 5
						}

						ListView {
							width: parent.width
							model: options

							delegate: Rectangle {
								width: filter.width
								height: 40

								color: optionState == "NONE" ? Style.filterOptionBackground :
									optionState == "POSITIVE" ? Style.filterOptionBackgroundPositive :
									optionState == "NEGATIVE" ? Style.filterOptionBackgroundNegative : "#000000"

								Text {
									text: name == "__PMP_OPTION_EVERYTHING__" ? "All" : name == "__PMP_OPTION_UNKNOWN__" ? "Unknown" : name
									font.pixelSize: 18
									font.family: Style.font
									color: Style.text
									horizontalAlignment: Text.AlignHCenter
									elide: Text.ElideRight
									anchors {
										verticalCenter: parent.verticalCenter
										left: parent.left
										right: parent.right
									}
								}

								Rectangle {
									anchors.fill: parent
									color: optionMouseArea.pressed ? Style.clicked : optionMouseArea.containsMouse ? Style.hover : "#00000000"
								}

								MouseArea {
									id: optionMouseArea
									anchors.fill: parent
									hoverEnabled: true
									acceptedButtons: Qt.LeftButton | Qt.RightButton

									onClicked: e => {
										if(e.button == Qt.LeftButton) {
											Interaction.filterOption(id, name, true)
										} else if(e.button == Qt.RightButton) {
											Interaction.filterOption(id, name, false)
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
