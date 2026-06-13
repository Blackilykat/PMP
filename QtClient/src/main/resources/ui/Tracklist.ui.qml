import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Rectangle {
    id: tracklist
    color: Style.tracklistBackground

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
                    Text {
                        elide: Text.ElideRight
                        text: name
                        width: headerWidth
                        color: Style.text
                        font.pixelSize: 22
                        horizontalAlignment: rightAligned ? Text.AlignRight : Text.AlignLeft
                        topPadding: 10
                        bottomPadding: 10


                    MouseArea {
                        anchors.fill: parent


                        onReleased: {
                            console.log(headerWidth)
                        }
                    }
                    }
                    Rectangle {
                        height: parent.height
                        width: 20
                        color: "#00000000"
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
                                width: trackHeadersModel.get(index).headerWidth
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
