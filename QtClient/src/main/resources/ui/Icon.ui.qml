import QtQuick
import QtQuick.Controls

// very silly way to do it but why is this not in qt quick
Button {
	id: btn
	property color color: Style.text
	property string source: ""

	palette {
		disabled.button: "#00000000"
	}

	enabled: false
	padding: 0

	icon {
		source: btn.source
		color: btn.color

		width: btn.width
		height: btn.height
	}
}

