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
import dev.blackilykat.pmp.client.Server
import dev.blackilykat.pmp.client.audio.AudioBackend
import dev.blackilykat.pmp.messages.LoginAsNewDeviceRequest
import dev.blackilykat.pmp.util.Globals
import java.io.File

class PMPApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Globals.dataRoot = getExternalFilesDir(null)
        Globals.library = File(Globals.dataRoot, "library")

        AudioBackend.backend = AndroidAudioBackend();

        Server.EVENT_SHOULD_ASK_PASSWORD.register {
            Server.send(LoginAsNewDeviceRequest("mypassword", "android"))
        }

        setupChannel()

        startService(Intent(this, PMPService::class.java))
    }

    private fun setupChannel() {

        val name = "Service notification"
        val descriptionText = "Notifies you when the app is running in the background"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel("serviceNotification", name, importance)
        mChannel.description = descriptionText
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }
}