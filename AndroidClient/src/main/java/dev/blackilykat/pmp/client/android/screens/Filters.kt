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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.blackilykat.pmp.client.ClientStorage
import dev.blackilykat.pmp.client.Filter
import dev.blackilykat.pmp.client.FilterOption
import dev.blackilykat.pmp.client.Library
import dev.blackilykat.pmp.client.android.Mutables
import dev.blackilykat.pmp.client.android.R
import dev.blackilykat.pmp.client.android.util.BoxedDropdownMenu
import dev.blackilykat.pmp.client.android.util.BoxedDropdownMenuItem
import sh.calvin.reorderable.ReorderableColumn


@Composable
fun Filters(paddingValues: PaddingValues) {
    Surface(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
        val selectedFilter by Mutables.selectedFilter
        val filters by Mutables.filters
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
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
                val deletePopupShown = remember { mutableStateOf(false) }
                val reorderPopupShown = remember { mutableStateOf(false) }
                val newPopupShown = remember { mutableStateOf(false) }
                Row {
                    FloatingActionButton(onClick = {
                        deletePopupShown.value = true
                    }, content = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete ${selectedFilter?.key}"
                        )
                    })

                    Spacer(Modifier.width(5.dp))

                    FloatingActionButton(onClick = {
                        reorderPopupShown.value = true
                    }, content = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_up_down_bold),
                            contentDescription = "Reorder filters"
                        )
                    })

                    Spacer(Modifier.width(5.dp))

                    FloatingActionButton(onClick = {
                        newPopupShown.value = true
                    }, content = {
                        Icon(
                            painter = painterResource(R.drawable.plus),
                            contentDescription = "New filter"
                        )
                    })
                }


                if (deletePopupShown.value) {
                    AlertDialog(
                        onDismissRequest = {
                            deletePopupShown.value = false
                        },
                        icon = {
                            Icon(painter = painterResource(R.drawable.delete), contentDescription = "delete")
                        },
                        title = { Text("Delete ${selectedFilter?.key}?") },
                        text = { Text("Are you sure you want to delete the filter for ${selectedFilter?.key}?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    Library.removeFilter(selectedFilter)
                                    deletePopupShown.value = false
                                }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = {
                                    deletePopupShown.value = false
                                }
                            ) {
                                Text("Dismiss")
                            }
                        }
                    )
                }

                var popupText by rememberSaveable { mutableStateOf("") }

                if (newPopupShown.value) {
                    AlertDialog(
                        onDismissRequest = {
                            newPopupShown.value = false
                        },
                        icon = {
                            Icon(painter = painterResource(R.drawable.plus), contentDescription = "create")
                        },
                        title = { Text("Add a filter") },
                        text = {
                            TextField(
                                value = popupText,
                                onValueChange = { popupText = it },
                                label = { Text("Metadata key") }
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    Library.addFilter(Filter(popupText))
                                    popupText = ""
                                    newPopupShown.value = false
                                }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = {
                                    newPopupShown.value = false
                                }
                            ) {
                                Text("Dismiss")
                            }
                        }
                    )
                }

                if (reorderPopupShown.value) {
                    AlertDialog(
                        onDismissRequest = {
                            reorderPopupShown.value = false
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.arrow_up_down_bold),
                                contentDescription = "reorder"
                            )
                        },
                        title = { Text("Reorder filters") },
                        text = {
                            ReorderableColumn(
                                list = filters,
                                onSettle = { from, to ->
                                    Library.moveFilter(ClientStorage.MAIN.filters!!.get(from), to)
                                },
                            ) { _, item, _ ->
                                key(item.id) {
                                    ReorderableItem {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .draggableHandle()
                                                .padding(5.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxHeight(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.drag_horizontal_variant),
                                                    "Reorder",
                                                    modifier = Modifier.size(30.dp).padding(end = 5.dp, start = 5.dp),
                                                )
                                                Text(item.key)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    reorderPopupShown.value = false
                                }
                            ) {
                                Text("Close")
                            }
                        },
                    )
                }
            }
        )
    }
}

@Composable
fun FilterOption(filterOption: FilterOption, state: FilterOption.State) {
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
