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

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.android.screens.Filters
import dev.blackilykat.pmp.client.android.screens.Playback
import dev.blackilykat.pmp.client.android.screens.Tracklist
import dev.blackilykat.pmp.client.android.screens.artistsString

class Screen(val name: String, val route: String, @DrawableRes val icon: Int)

val screens = listOf(
    Screen("Playback", "playback", R.drawable.baseline_music_note_24),
    Screen("Tracks", "tracklist", R.drawable.playlist_play),
    Screen("Filters", "filters", R.drawable.filter),
    Screen("Settings", "settings", R.drawable.cog),
)

private class IndexedScreen(val index: Int, val screen: Screen)

fun NavController.doNavigate(route: String, selected: MutableIntState) {
    selected.intValue = screens
        .mapIndexed { index, screen -> IndexedScreen(index, screen) }
        .filter { it.screen.route == route }
        .map { it.index }
        .firstOrNull() ?: -1

    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun Navigation(activity: MainActivity) {
    val selected = rememberSaveable { mutableIntStateOf(0) }
    val navController = rememberNavController()

    showDialogs()

    Scaffold(
        bottomBar = {
            Column {
                if (shouldShowPlayBar(selected)) {
                    PlayBar(navController, selected)
                }
                NavigationBar {
                    screens.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = selected.intValue == index,
                            label = {
                                Text(screen.name)
                            },
                            onClick = {
                                navController.doNavigate(screen.route, selected)
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
        }
    ) { paddingValues ->
        val paddingValues = if (shouldShowPlayBar(selected)) {
            paddingValues.minus(
                PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        } else paddingValues
        NavHost(
            navController = navController,
            startDestination = "playback",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            composable("playback") {
                BackHandler { navController.doNavigate("tracklist", selected) }
                Playback(paddingValues)
            }
            composable("tracklist") {
                Tracklist(paddingValues, activity)
            }
            composable("filters") {
                BackHandler { navController.doNavigate("tracklist", selected) }
                Filters(paddingValues)
            }
            composable("settings") {
                BackHandler { navController.doNavigate("tracklist", selected) }
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Text("TODO: settings")
                    }
                }
            }
        }
    }

}

fun shouldShowPlayBar(selected: MutableIntState) = selected.intValue != 0

@Composable
fun PlayBar(navController: NavController, selected: MutableIntState) {
    val playing by Mutables.playing
    val track by Mutables.currentTrack
    val albumArt by Mutables.albumArt

    val idealHeight = 75.dp
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            navController.doNavigate("playback", selected)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            albumArt?.let {
                Image(
                    bitmap = BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap(),
                    contentDescription = "Album art",
                    modifier = Modifier.fillMaxHeight().widthIn(0.dp, idealHeight).aspectRatio(1f)
                )
            }

            Column(
                modifier = Modifier.fillMaxHeight().padding(10.dp).weight(1f),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = track?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )

                Text(
                    text = artistsString(track),
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )

            }

            val playPauseSize = 50.dp

            val playPausePadding = (idealHeight - playPauseSize) / 2

            IconButton(
                onClick = {
                    Player.playPause()
                },
                modifier = Modifier
                    .padding(
                        top = playPausePadding,
                        bottom = playPausePadding,
                        end = playPausePadding
                    )
                    .fillMaxHeight()
                    .size(playPauseSize)
            ) {
                if (playing) {
                    Icon(
                        painter = painterResource(R.drawable.pause),
                        contentDescription = "pause",
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = "play",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}