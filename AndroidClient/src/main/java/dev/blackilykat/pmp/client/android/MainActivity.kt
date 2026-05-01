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

package dev.blackilykat.pmp.client.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.Main
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.Server
import dev.blackilykat.pmp.client.android.theme.PMPTheme
import dev.blackilykat.pmp.client.audio.AudioBackend
import dev.blackilykat.pmp.messages.LoginAsNewDeviceRequest
import dev.blackilykat.pmp.util.Globals
import dev.blackilykat.pmp.util.Shutdown
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO: move init stuff to PMPApplication
        Globals.dataRoot = getExternalFilesDir(null)
        Globals.library = File(Globals.dataRoot, "library")

        AudioBackend.backend = AndroidAudioBackend();

        Server.EVENT_SHOULD_ASK_PASSWORD.register {
            Server.send(LoginAsNewDeviceRequest("mypassword", "android"))
        }

        Main.main(emptyArray<String>())

        val playing = mutableStateOf(false)

        Player.EVENT_PLAY_PAUSE.register {
            playing.value = !it
        }

        val shuffle = mutableStateOf(ShuffleOption.OFF)

        Player.EVENT_SHUFFLE_CHANGED.register {
            shuffle.value = it
        }

        val repeat = mutableStateOf(RepeatOption.OFF)

        Player.EVENT_REPEAT_CHANGED.register {
            repeat.value = it
        }

        val title = mutableStateOf("title")

        Player.EVENT_TRACK_CHANGE.register {
            title.value = it.track?.title.toString()
        }

        setContent {
            PMPTheme {
                Column {
                    TestText()
                    TestText()
                    TestText()
                    TestText()
                    TestText()
                    TestText()
                    TestText()
                    val playing by playing
                    val shuffle by shuffle
                    val repeat by repeat
                    val title by title

                    Button(onClick = {
                        Player.setShuffle(
                            when (Player.getShuffle()) {
                                ShuffleOption.ON -> ShuffleOption.OFF
                                ShuffleOption.OFF -> ShuffleOption.ON
                            }
                        )
                    }, content = { Text("Shuffle: $shuffle") })

                    Row {
                        Button(onClick = { Player.previous() }, content = { Text("«") })
                        Button(onClick = {
                            Player.playPause()
                        }, content = { Text(if (playing) "Pause" else "Play") })
                        Button(onClick = { Player.next() }, content = { Text("»") })
                    }

                    Button(onClick = {
                        Player.setRepeat(
                            when (Player.getRepeat()) {
                                RepeatOption.OFF -> RepeatOption.ALL
                                RepeatOption.ALL -> RepeatOption.TRACK
                                RepeatOption.TRACK -> RepeatOption.OFF
                            }
                        )
                    }, content = { Text("Repeat: $repeat") })

                    Text("Playing: $title")


                    Button(onClick = {
                        Player.takeOwnership()
                    }, content = { Text("Take ownership") })

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Shutdown.mayShutdownSoon()
    }
}

@Composable
fun TestText() {
    Text("PMP")
}