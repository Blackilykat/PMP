import QtQuick
import QtQuick.Controls.Basic
import "."

Window {
    id: root

    color: Style.tracklistBackground

    palette {
        text: Style.text
        windowText: Style.text

        button: Style.buttonBackground
        buttonText: Style.text
    }

    visible: true
    title: "PMP"

    Tracklist {
        id: tracklist
        objectName: "tracklist"
        anchors {
            top: parent.top
            left: parent.left
            right: parent.right
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

}

