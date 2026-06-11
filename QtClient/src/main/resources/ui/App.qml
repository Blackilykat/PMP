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

    property string test: "hi"

    visible: true
    title: "PMP"

    Text {
        id: whatever
        text: root.test

    }
    Playbar {
        id: playbar
        objectName: "playbar"
        anchors.bottom: parent.bottom
        anchors.left: parent.left
        anchors.right: parent.right
    }

}

