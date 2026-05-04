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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.android.screens.Filters
import dev.blackilykat.pmp.client.android.screens.Tracklist

class Screen(val name: String, val route: String, @DrawableRes val icon: Int)

val screens = listOf(
    Screen("Playback", "playback", R.drawable.baseline_music_note_24),
    Screen("Tracks", "tracklist", R.drawable.playlist_play),
    Screen("Filters", "filters", R.drawable.filter),
    Screen("Settings", "settings", R.drawable.cog),
)

@Composable
fun Navigation() {
    var selected by rememberSaveable { mutableIntStateOf(1) }
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selected == index,
                        label = {
                            Text(screen.name)
                        },
                        onClick = {
                            selected = index
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(screen.icon),
                                contentDescription = screen.name
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "tracklist",
        ) {
            composable("playback") {
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Text("TODO: playback")
                    }
                }
            }
            composable("tracklist") {
                Tracklist(paddingValues)
            }
            composable("filters") {
                Filters(paddingValues)
            }
            composable("settings") {

                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Text("TODO: settings")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayBar() {
    val playing by Mutables.playing
    val shuffle by Mutables.shuffle
    val repeat by Mutables.repeat
    val track by Mutables.currentTrack
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
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
        }
    }
}