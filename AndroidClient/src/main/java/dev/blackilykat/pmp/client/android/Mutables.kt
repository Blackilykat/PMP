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

import android.annotation.SuppressLint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.Library
import dev.blackilykat.pmp.client.Player
import dev.blackilykat.pmp.client.Track

class Mutables {
    companion object {
        val playing = mutableStateOf(false)
        val shuffle = mutableStateOf(ShuffleOption.OFF)
        val repeat = mutableStateOf(RepeatOption.OFF)
        val currentTrack: MutableState<Track?> = mutableStateOf(null)

        @SuppressLint("MutableCollectionMutableState") // the list gets manually updated through the listener
        val tracks = mutableStateOf(Library.getSelectedTracks())

        init {
            Player.EVENT_PLAY_PAUSE.register { playing.value = !it }
            Player.EVENT_SHUFFLE_CHANGED.register { shuffle.value = it }
            Player.EVENT_REPEAT_CHANGED.register { repeat.value = it }
            Player.EVENT_TRACK_CHANGE.register { currentTrack.value = it.track }
            Library.EVENT_SELECTED_TRACKS_UPDATED.register { tracks.value = it.newSelection }
        }
    }
}