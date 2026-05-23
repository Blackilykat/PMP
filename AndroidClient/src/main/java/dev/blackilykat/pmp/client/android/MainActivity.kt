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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import dev.blackilykat.pmp.client.Library
import dev.blackilykat.pmp.client.android.theme.PMPTheme
import dev.blackilykat.pmp.util.Shutdown
import java.io.IOException
import java.nio.file.FileAlreadyExistsException

class MainActivity : ComponentActivity() {
    var addTracksLauncher: ActivityResultLauncher<String>? = null

    fun addTracksPopup() {
        // x-flac does not work
        addTracksLauncher?.launch("audio/flac")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addTracksLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { it ->
            for (uri in it) {
                contentResolver.openInputStream(uri).use { file ->
                    try {
                        if (Library.addTrack(file)) {
                            println("Added track $uri")
                        } else {
                            println("Skipped track $uri as it doesn't seem to be a valid FLAC file")
                        }
                    } catch (_: FileAlreadyExistsException) {
                        println("Skipped track $uri as it already exists");
                    } catch (ex: IOException) {
                        println("Failed to add track $uri");
                        ex.printStackTrace()
                    }
                };
            }
        }

        setContent {
            PMPTheme {
                Navigation(this)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Shutdown.mayShutdownSoon()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}