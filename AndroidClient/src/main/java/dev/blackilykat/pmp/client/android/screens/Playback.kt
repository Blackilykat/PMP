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

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.android.Mutables
import dev.blackilykat.pmp.client.android.R

@Composable
fun Playback(paddingValues: PaddingValues) {
    val playing by Mutables.playing
    val shuffle by Mutables.shuffle
    val repeat by Mutables.repeat
    val track by Mutables.currentTrack
    val albumArt by Mutables.albumArt
    val position by Mutables.position

    val artists = track?.let { track ->
        track.metadata.filter { it.key.lowercase() == "artist" }.joinToString { it.value }
    } ?: ""

    Scaffold(
        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                Player.setShuffle(
                                    when (Player.getShuffle()) {
                                        ShuffleOption.ON -> ShuffleOption.OFF
                                        ShuffleOption.OFF -> ShuffleOption.ON
                                    }
                                )
                            },
                        ) {
                            when (shuffle) {
                                ShuffleOption.ON -> Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = "Shuffle on",
                                    modifier = Modifier.size(40.dp)
                                )

                                ShuffleOption.OFF -> Icon(
                                    painter = painterResource(R.drawable.shuffle_disabled),
                                    contentDescription = "Shuffle off",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        IconButton(
                            onClick = { Player.previous() },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = "Previous track",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        IconButton(
                            onClick = { Player.playPause() },
                            colors = IconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.primaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                disabledContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.size(80.dp)
                        ) {
                            if (playing) {
                                Icon(
                                    painter = painterResource(R.drawable.pause),
                                    contentDescription = "Pause",
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = "Play",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        IconButton(
                            onClick = { Player.next() },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = "Next track",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        IconButton(
                            onClick = {
                                Player.setRepeat(
                                    when (Player.getRepeat()) {
                                        RepeatOption.OFF -> RepeatOption.ALL
                                        RepeatOption.ALL -> RepeatOption.TRACK
                                        RepeatOption.TRACK -> RepeatOption.OFF
                                    }
                                )
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            when (repeat) {
                                RepeatOption.OFF -> Icon(
                                    painter = painterResource(R.drawable.repeat_off),
                                    contentDescription = "repeat off",
                                    modifier = Modifier.size(40.dp)
                                )

                                RepeatOption.ALL -> Icon(
                                    painter = painterResource(R.drawable.repeat),
                                    contentDescription = "repeat all",
                                    modifier = Modifier.size(40.dp)
                                )

                                RepeatOption.TRACK -> Icon(
                                    painter = painterResource(R.drawable.repeat_once),
                                    contentDescription = "repeat this track",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    val seeking: MutableState<Float?> = remember { mutableStateOf(null) }

                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatSeconds(position / 1000))
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier.padding(5.dp).weight(1f),
                        ) {
                            val progress = (seeking.value ?: position.toFloat())
                            val max = ((track?.durationSeconds ?: 0).toFloat() * 1000)

                            LinearWavyProgressIndicator(
                                progress = { progress / max },
                                waveSpeed = WavyProgressIndicatorDefaults.LinearDeterminateWavelength / 2f,
                                amplitude = { if (playing) 0.2f else 0f },
                                modifier = Modifier.height(48.dp).padding(start = 8.dp, end = 8.dp),
                                trackColor = Color.Transparent,
                                stopSize = 0.dp,
                            )

                            Slider(
                                value = progress,
                                valueRange = 0f..max,
                                onValueChange = {
                                    seeking.value = it
                                },
                                onValueChangeFinished = {
                                    seeking.value?.let {
                                        Player.seek(it.toLong())
                                        seeking.value = null
                                    }
                                },
                                colors = SliderColors(
                                    thumbColor = SliderDefaults.colors().thumbColor,
                                    activeTrackColor = Color.Transparent,
                                    activeTickColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                    disabledThumbColor = Color.Transparent,
                                    disabledActiveTrackColor = Color.Transparent,
                                    disabledActiveTickColor = Color.Transparent,
                                    disabledInactiveTrackColor = Color.Transparent,
                                    disabledInactiveTickColor = Color.Transparent,
                                ),
                                thumb = {
                                    SliderDefaults.Thumb(
                                        interactionSource = remember { MutableInteractionSource() },
                                        thumbSize = DpSize(20.dp, 20.dp),
                                    )
                                }
                            )
                        }
                        Text(formatSeconds(track?.durationSeconds?.toLong() ?: 0))
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(paddingValues = paddingValues)
            ) {
                albumArt?.let {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f).padding(bottom = 20.dp)
                        )
                    }
                }

                Text(
                    text = track?.title ?: "",
                    fontSize = 10.em,
                    textAlign = TextAlign.Center,
                    lineHeight = 1.em,
                    modifier = Modifier.fillMaxWidth().padding(
                        top = 0.dp,
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 0.dp
                    )
                )

                Text(
                    text = artists,
                    fontSize = 6.em,
                    textAlign = TextAlign.Center,
                    lineHeight = 1.em,
                    modifier = Modifier.fillMaxWidth().padding(
                        top = 5.dp,
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 0.dp
                    )
                )
            }
        }
    }
}

fun formatSeconds(seconds: Long): String {
    var minutes = seconds / 60;
    val seconds = seconds % 60;
    val hours = minutes / 60;
    minutes %= 60;

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}