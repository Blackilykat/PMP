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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.blackilykat.pmp.Order
import dev.blackilykat.pmp.client.Library
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.Track
import dev.blackilykat.pmp.client.android.Mutables
import dev.blackilykat.pmp.client.android.PlayBar
import dev.blackilykat.pmp.client.android.R
import dev.blackilykat.pmp.client.android.util.BoxedDropdownMenu
import dev.blackilykat.pmp.client.android.util.BoxedDropdownMenuItem

@Composable
fun Tracklist(paddingValues: PaddingValues) {
    Surface(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val headers by Mutables.headers
                val sortingHeader by Mutables.sortingHeader
                val sortingOrder by Mutables.sortingOrder
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .fillMaxWidth()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                    ) {
                        BoxedDropdownMenu(
                            modifier = Modifier.weight(1f),
                            items = headers.map {
                                BoxedDropdownMenuItem(
                                    onSelected = { Library.setSorting(it, Order.ASCENDING) },
                                    selected = sortingHeader == it
                                ) {
                                    Text("Sort by ${it.label}")
                                }
                            }
                        )

                        Spacer(Modifier.width(5.dp))

                        IconButton(
                            modifier = Modifier.height(IntrinsicSize.Max),
                            onClick = {
                                Library.setSorting(
                                    Library.getSortingHeader(),
                                    when (Library.getSortingOrder()) {
                                        Order.ASCENDING -> Order.DESCENDING
                                        Order.DESCENDING -> Order.ASCENDING
                                    }
                                )
                            }
                        ) {
                            when (sortingOrder) {
                                Order.ASCENDING -> Icon(
                                    painter = painterResource(R.drawable.sort_ascending),
                                    contentDescription = "Sort: ascending"
                                )

                                Order.DESCENDING -> Icon(
                                    painter = painterResource(R.drawable.sort_descending),
                                    contentDescription = "Sort: descending"
                                )
                            }
                        }
                    }
                }
            },
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
        color = if (playing == track) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(5.dp)),
        onClick = {
            Player.play(track)
        }
    ) {
        Row {
            Column(
                modifier = Modifier.padding(10.dp).weight(1f)
            ) {
                Row(
                ) {
                    Text(
                        text = track.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 6.em
                    )

                    Text(
                        text = formatSeconds(track.durationSeconds.toLong()),
                        fontSize = 5.em,
                        textAlign = TextAlign.Right,
                    )
                }

                val artists = track.metadata.filter { it.key.lowercase() == "artist" }.joinToString { it.value }
                Text(
                    text = artists,
                    fontSize = 4.em,
                )
            }
            TrackMenu(track)
        }
    }
}

@Composable
fun TrackMenu(track: Track) {
    val menuExpanded = remember { mutableStateOf(false) }
    val deletePopupShown = remember { mutableStateOf(false) }

    if (deletePopupShown.value) {
        AlertDialog(
            onDismissRequest = {
                deletePopupShown.value = false
            },
            icon = {
                Icon(painter = painterResource(R.drawable.delete), contentDescription = "delete")
            },
            title = { Text("Delete ${track.title}?") },
            text = { Text("Are you sure you want to delete ${track.title}? This action is unrecoverable.") },
            confirmButton = {
                Button(
                    onClick = {
                        Library.removeTrack(track)
                        deletePopupShown.value = false
                        menuExpanded.value = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        deletePopupShown.value = false
                        menuExpanded.value = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    IconButton(
        onClick = {
            menuExpanded.value = true
        }
    ) {
        DropdownMenu(
            expanded = menuExpanded.value,
            onDismissRequest = { menuExpanded.value = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    deletePopupShown.value = true
                }
            )
        }

        Icon(
            painter = painterResource(R.drawable.dots_vertical),
            contentDescription = "more"
        )
    }
}