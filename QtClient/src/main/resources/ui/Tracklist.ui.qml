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
                console.log(tracklist.tracks)
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

            delegate: Row {
                Repeater {
                    id: thedata

                    model: metadata

                    delegate: Text {
                        text: value
                        color: Style.text
                        font.pixelSize: 16
                    }
                }
            }
        }

    }
}
