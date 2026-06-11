import QtQuick

Rectangle {
    id: timebar
    color: "#00000000"

    property double progress: 0

    property real mouseX: 0

    Rectangle {
        id: track
        height: 4
        color: palette.windowText
        border.width: 0

        anchors {
            verticalCenter: parent.verticalCenter
            left: parent.left
            right: parent.right
        }
    }

    Rectangle {
        id: thumb
        width: 9
        color: palette.windowText
        radius: 10

        anchors {
            left: parent.left
            top: parent.top
            bottom: parent.bottom
            leftMargin: (mousearea.pressed ? timebar.mouseX : timebar.progress * timebar.width) - (width / 2)
        }
    }

    MouseArea {
        id: mousearea
        anchors.fill: parent

        onPositionChanged: {
            if(mouse.x < 0) {
                timebar.mouseX = 0
            } else if(mouse.x > width) {
                timebar.mouseX = width
            } else {
                timebar.mouseX = mouse.x
            }
        }

        onPressed: {
            timebar.mouseX = mouse.x
        }

        onReleased: {
            Interaction.seek(mouse.x / width)
        }
    }
}
