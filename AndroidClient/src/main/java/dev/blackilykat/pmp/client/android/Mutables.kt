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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.*
import dev.blackilykat.pmp.event.Listener

class OptionAndState(val option: FilterOption, var state: FilterOption.State)

class Mutables {
    companion object {
        val playing = mutableStateOf(false)
        val shuffle = mutableStateOf(ShuffleOption.OFF)
        val repeat = mutableStateOf(RepeatOption.OFF)
        val currentTrack: MutableState<Track?> = mutableStateOf(null)

        @SuppressLint("MutableCollectionMutableState") // the list gets manually updated through the listener
        val tracks = mutableStateOf(Library.getSelectedTracks())

        val headers: MutableState<List<Header>> = mutableStateOf(emptyList())
        val sortingHeader = mutableStateOf(Library.getSortingHeader())
        val sortingOrder = mutableStateOf(Library.getSortingOrder())

        val filters: MutableState<List<Filter>> = mutableStateOf(emptyList())
        val selectedFilter: MutableState<Filter?> = mutableStateOf(filters.value.firstOrNull())
        val selectedFilterOptions: MutableState<List<OptionAndState>> = mutableStateOf(emptyList())

        val selectedFilterAddListener: Listener<Filter.OptionAddedEvent> = {
            updateSelectedFilterOptions()
        }

        val selectedFilterRemoveListener: Listener<Filter.OptionRemovedEvent> = {
            updateSelectedFilterOptions()
        }

        val albumArt: MutableState<ByteArray?> = mutableStateOf(null)
        val position = mutableLongStateOf(0)

        fun init() {
            headers.value = ClientStorage.MAIN.headers.get()
            filters.value = ClientStorage.MAIN.filters.get()
        }

        init {
            Player.EVENT_PLAY_PAUSE.register { playing.value = !it }
            Player.EVENT_SHUFFLE_CHANGED.register { shuffle.value = it }
            Player.EVENT_REPEAT_CHANGED.register { repeat.value = it }
            Player.EVENT_TRACK_CHANGE.register {
                currentTrack.value = it.track
                it.picture.thenAccept { data ->
                    albumArt.value = data
                }
            }
            Player.EVENT_PROGRESS.register {
                position.longValue = it
            }
            Library.EVENT_SELECTED_TRACKS_UPDATED.register { tracks.value = it.newSelection }
            Library.EVENT_SORTING_HEADER_UPDATED.register {
                sortingHeader.value = it.header
                sortingOrder.value = it.order
            }
            Library.EVENT_HEADERS_UPDATED.register { headers.value = it }
            Library.EVENT_FILTERS_UPDATED.register { filters.value = ArrayList(it) }

            selectedFilter.value?.eventOptionAdded?.register(selectedFilterAddListener)
            selectedFilter.value?.eventOptionRemoved?.register(selectedFilterRemoveListener)
            Filter.EVENT_OPTION_CHANGED_STATE.register {
                if (it.filter != selectedFilter.value) return@register

                updateSelectedFilterOptions()

            }

            updateSelectedFilterOptions()
        }

        fun setSelectedFilter(newValue: Filter) {
            selectedFilter.value?.eventOptionAdded?.unregister(selectedFilterAddListener)
            selectedFilter.value?.eventOptionRemoved?.unregister(selectedFilterRemoveListener)

            selectedFilter.value = newValue

            newValue.eventOptionAdded.unregister(selectedFilterAddListener)
            newValue.eventOptionRemoved.unregister(selectedFilterRemoveListener)

            updateSelectedFilterOptions()
        }

        fun updateSelectedFilterOptions() {
            selectedFilter.value?.let { selectedFilter ->
                selectedFilterOptions.value = selectedFilter.options.map { OptionAndState(it, it.state) }
            }
        }
    }
}