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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import dev.blackilykat.pmp.client.Server

// All dialogs that should show up globally on any screen.

@Composable
fun showDialogs() {
    // only show one dialog at a time
    if (passwordDialog()) return
}

@Composable
private fun passwordDialog(): Boolean {
    if (Mutables.shouldAskPassword.value) {
        val state = remember { TextFieldState() }
        AlertDialog(
            onDismissRequest = {
                Server.disconnectSoonWithoutRetrying("User dismissed password dialog")
                Mutables.shouldAskPassword.value = false
            },
            icon = {
                Icon(painter = painterResource(R.drawable.form_textbox_password), contentDescription = "password")
            },
            title = { Text("Server password required") },
            text = {
                SecureTextField(
                    state = state,
                    textObfuscationMode = TextObfuscationMode.RevealLastTyped,
                    label = { Text("Password") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Server.submitPassword(state.text.toString())
                        Mutables.shouldAskPassword.value = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        Server.disconnectSoonWithoutRetrying("User dismissed password dialog")
                        Mutables.shouldAskPassword.value = false
                    }
                ) {
                    Text("Disconnect")
                }
            }
        )
        return true
    }
    return false
}