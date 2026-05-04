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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.blackilykat.pmp.client.Filter
import dev.blackilykat.pmp.client.FilterOption
import dev.blackilykat.pmp.client.android.Mutables
import dev.blackilykat.pmp.client.android.PlayBar
import dev.blackilykat.pmp.client.android.R
import dev.blackilykat.pmp.client.android.util.BoxedDropdownMenu
import dev.blackilykat.pmp.client.android.util.BoxedDropdownMenuItem
import dev.blackilykat.pmp.client.android.util.TodoPopup


@Composable
fun Filters(paddingValues: PaddingValues) {
    Surface(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val filters by Mutables.filters
                val selectedFilter by Mutables.selectedFilter
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
                            items = filters.map {
                                BoxedDropdownMenuItem(
                                    onSelected = { Mutables.setSelectedFilter(it) },
                                    selected = selectedFilter == it
                                ) {
                                    Text(it.key)
                                }
                            }
                        )
                    }
                }
            },
            bottomBar = { PlayBar() },
            content = { paddingValues ->
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.padding(paddingValues = paddingValues).fillMaxSize()
                ) {
                    val selectedFilterOptions by Mutables.selectedFilterOptions
                    LazyColumn {
                        items(selectedFilterOptions) {
                            FilterOption(it.option, it.state)
                        }
                    }
                }
            },
            floatingActionButton = {
                val todo = TodoPopup()
                Row {
                    FloatingActionButton(onClick = {
                        todo.value = true
                    }, content = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete {}"
                        )
                    })

                    Spacer(Modifier.width(5.dp))

                    FloatingActionButton(onClick = {
                        todo.value = true
                    }, content = {
                        Icon(
                            painter = painterResource(R.drawable.pencil),
                            contentDescription = "Edit {}"
                        )
                    })

                    Spacer(Modifier.width(5.dp))

                    FloatingActionButton(onClick = {
                        todo.value = true
                    }, content = {
                        Icon(
                            painter = painterResource(R.drawable.plus),
                            contentDescription = "New filter"
                        )
                    })
                }
            }
        )
    }
}

@Composable
fun FilterOption(filterOption: FilterOption, state: FilterOption.State) {
    println("State: $state")
    Surface(
        color = when (state) {
            FilterOption.State.POSITIVE -> MaterialTheme.colorScheme.secondaryContainer
            FilterOption.State.NEGATIVE -> MaterialTheme.colorScheme.errorContainer
            FilterOption.State.NONE -> MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(5.dp)),
        onClick = {
            filterOption.state = when (filterOption.state) {
                FilterOption.State.NONE -> FilterOption.State.POSITIVE
                FilterOption.State.POSITIVE -> FilterOption.State.NEGATIVE
                FilterOption.State.NEGATIVE -> FilterOption.State.NONE
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            when (filterOption.value) {
                Filter.OPTION_EVERYTHING -> Text("All")
                Filter.OPTION_UNKNOWN -> Text("Unknown")
                else -> Text(filterOption.value)
            }
        }
    }
}
