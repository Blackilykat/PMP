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

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import dev.blackilykat.pmp.Globals
import dev.blackilykat.pmp.client.audio.AudioBackend
import java.io.File

class PMPApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Globals.dataRoot = getExternalFilesDir(null)
        Globals.library = File(Globals.dataRoot, "library")

        AudioBackend.backend = AndroidAudioBackend()

        setupChannel()

        startForegroundService(Intent(this, PMPService::class.java))
    }

    private fun setupChannel() {
        val service = NotificationChannel(
            "serviceNotification",
            "Service Notification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        service.description = "Notifies you when the app is running in the background"

        val media = NotificationChannel(
            "mediaNotification",
            "Media Notification",
            NotificationManager.IMPORTANCE_NONE
        )
        media.description = "Allows controlling playback from your notifications"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(service)
        notificationManager.createNotificationChannel(media)
    }
}
