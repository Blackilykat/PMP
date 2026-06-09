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

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.util.UnstableApi
import dev.blackilykat.pmp.client.Main

/**
 * PMP is expected to keep running in the background for playback and to stay connected with the server.
 */
class PMPService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("PMPService starting")

        val notification = NotificationCompat.Builder(this, "serviceNotification")
            .setOngoing(true)
            .setContentTitle("PMP")
            .setContentText("PMP is running in the background.")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.pmp)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        ServiceCompat.startForeground(this, 100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        Main.main(emptyArray<String>())

        Mutables.init()

        initMediaControls(this)

        return super.onStartCommand(intent, flags, startId)
    }
}