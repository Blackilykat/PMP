import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Rectangle {
    id: tracklist
    color: Style.tracklistBackground

    Rectangle {
        id: header
        color: Style.panelBackground
        height: 400
        z: 1
        anchors {
            top: parent.top
            left: parent.left
            right: parent.right
        }


        Text {
            text: "Header"
            color: Style.text

            font.pixelSize: 100
        }

        MouseArea {
            anchors.fill: parent


            onReleased: {
                console.log(headerWidths[1])
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
                        width: 40
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

                        delegate: Text {
                            elide: Text.ElideRight
                            text: value
                            width: headerWidths[index]
                            color: Style.text
                            font.pixelSize: 22
                            horizontalAlignment: rightAligned ? Text.AlignRight : Text.AlignLeft
                            padding: 10
                        }
                    }
                }

                MouseArea {
                    anchors.fill: parent
                    onReleased: {
                        Interaction.playTrack(filename)
                    }
                }
            }
        }

    }
}
