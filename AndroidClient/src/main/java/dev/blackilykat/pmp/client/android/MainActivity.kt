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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.android.theme.PMPTheme
import dev.blackilykat.pmp.event.Listener
import dev.blackilykat.pmp.util.Shutdown

class MainActivity : ComponentActivity() {
    val playing = mutableStateOf(false)
    val playPauseListener: Listener<Boolean> = { playing.value = !it }
    val shuffle = mutableStateOf(ShuffleOption.OFF)
    val shuffleChangedListener: Listener<ShuffleOption> = { shuffle.value = it }
    val repeat = mutableStateOf(RepeatOption.OFF)
    val repeatChangedListener: Listener<RepeatOption> = { repeat.value = it }
    val title = mutableStateOf("title")
    val trackChangeListener: Listener<Player.TrackChangeEvent> = {
        title.value = it.track?.title.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Player.EVENT_PLAY_PAUSE.register(playPauseListener)
        Player.EVENT_SHUFFLE_CHANGED.register(shuffleChangedListener)
        Player.EVENT_REPEAT_CHANGED.register(repeatChangedListener)
        Player.EVENT_TRACK_CHANGE.register(trackChangeListener)

        setContent {
            PMPTheme {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Scaffold(
                        topBar = { TestText() },
                        content = { padding ->

                            val playing by playing
                            val shuffle by shuffle
                            val repeat by repeat
                            val title by title
                            LazyColumn(modifier = Modifier.padding(padding)) {

                                for (i in 1..15) {
                                    item(key = i) {
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
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            Button(onClick = {
                                Player.takeOwnership()
                            }, content = { Text("Take ownership") })
                        }
                    )

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Shutdown.mayShutdownSoon()
    }

    override fun onDestroy() {
        super.onDestroy()
        Player.EVENT_PLAY_PAUSE.unregister(playPauseListener)
        Player.EVENT_SHUFFLE_CHANGED.unregister(shuffleChangedListener)
        Player.EVENT_REPEAT_CHANGED.unregister(repeatChangedListener)
        Player.EVENT_TRACK_CHANGE.unregister(trackChangeListener)
    }
}

@Composable
fun TestText() {
    Text("PMP")
}