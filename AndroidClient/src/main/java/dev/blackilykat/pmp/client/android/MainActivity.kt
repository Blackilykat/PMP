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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.Library
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.Track
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
    val currentTrack: MutableState<Track?> = mutableStateOf(null)
    val trackChangeListener: Listener<Player.TrackChangeEvent> = {
        currentTrack.value = it.track
    }
    val tracks = mutableStateOf(Library.getSelectedTracks())
    val trackListListener: Listener<Library.SelectedTracksUpdatedEvent> = {
        tracks.value = it.newSelection
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Player.EVENT_PLAY_PAUSE.register(playPauseListener)
        Player.EVENT_SHUFFLE_CHANGED.register(shuffleChangedListener)
        Player.EVENT_REPEAT_CHANGED.register(repeatChangedListener)
        Player.EVENT_TRACK_CHANGE.register(trackChangeListener)
        Library.EVENT_SELECTED_TRACKS_UPDATED.register(trackListListener)

        setContent {
            PMPTheme {
                Surface {
                    Scaffold(
                        topBar = { TestText() },
                        content = { padding ->

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth().fillMaxHeight()
                            ) {
                                val tracks by tracks
                                LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth()) {
                                    items(items = tracks) {
                                        Track(it)
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            PlayBar()
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

    @Composable
    fun PlayBar() {
        val playing by playing
        val shuffle by shuffle
        val repeat by repeat
        val track by currentTrack
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Playing: ${track?.title}")
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Button(onClick = {
                        Player.setShuffle(
                            when (Player.getShuffle()) {
                                ShuffleOption.ON -> ShuffleOption.OFF
                                ShuffleOption.OFF -> ShuffleOption.ON
                            }
                        )
                    }, content = {
                        when (shuffle) {
                            ShuffleOption.ON -> Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = "Shuffle on"
                            )

                            ShuffleOption.OFF -> Icon(
                                painter = painterResource(R.drawable.shuffle_disabled),
                                contentDescription = "Shuffle off"
                            )
                        }
                    })

                    Button(onClick = { Player.previous() }, content = {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "previous"
                        )
                    })

                    Button(onClick = {
                        Player.playPause()
                    }, content = {
                        if (playing) {
                            Icon(
                                painter = painterResource(R.drawable.pause),
                                contentDescription = "pause"
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = "play"
                            )
                        }
                    })
                    Button(onClick = { Player.next() }, content = {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "next"
                        )
                    })


                    Button(onClick = {
                        Player.setRepeat(
                            when (Player.getRepeat()) {
                                RepeatOption.OFF -> RepeatOption.ALL
                                RepeatOption.ALL -> RepeatOption.TRACK
                                RepeatOption.TRACK -> RepeatOption.OFF
                            }
                        )
                    }, content = {
                        when (repeat) {
                            RepeatOption.OFF -> Icon(
                                painter = painterResource(R.drawable.repeat_off),
                                contentDescription = "repeat off"
                            )

                            RepeatOption.ALL -> Icon(
                                painter = painterResource(R.drawable.repeat),
                                contentDescription = "repeat all"
                            )

                            RepeatOption.TRACK -> Icon(
                                painter = painterResource(R.drawable.repeat_once),
                                contentDescription = "repeat this track"
                            )
                        }
                    })
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    @Composable
    fun Track(track: Track) {
        val playing by currentTrack
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .clip(RoundedCornerShape(5.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(5.dp)
                ),
            onClick = {
                Player.play(track)
            }
        ) {
            Column(
                modifier = Modifier.padding(5.dp)

            ) {
                if (playing == track) {
                    Text("(playing)")
                }
                Text(track.title)

                val artists = remember {
                    track.metadata.filter { it.key.lowercase() == "artist" }.joinToString { it.value }
                }

                Text(artists)
            }
        }
    }
}

@Composable
fun TestText() {
    Text("PMP")
}
