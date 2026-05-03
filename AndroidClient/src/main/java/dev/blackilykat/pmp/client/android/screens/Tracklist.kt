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

package dev.blackilykat.pmp.client.android.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.Track
import dev.blackilykat.pmp.client.android.Mutables
import dev.blackilykat.pmp.client.android.PlayBar
import dev.blackilykat.pmp.client.android.TestText

@Composable
fun Tracklist(paddingValues: PaddingValues) {
    Surface(modifier = Modifier.padding(paddingValues)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TestText() },
            bottomBar = { PlayBar() },
            content = { padding ->
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                ) {
                    val tracks by Mutables.tracks
                    LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth()) {
                        items(items = tracks) {
                            Track(it)
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

@Composable
fun Track(track: Track) {
    val playing by Mutables.currentTrack
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

            val artists = track.metadata.filter { it.key.lowercase() == "artist" }.joinToString { it.value }

            Text(artists)
        }
    }
}