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

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaMetadata
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.blackilykat.pmp.RepeatOption
import dev.blackilykat.pmp.ShuffleOption
import dev.blackilykat.pmp.client.Player
import androidx.media3.common.Player as AndroidPlayer

private var albumArt: ByteArray? = null

@UnstableApi
class PMPPlayer(looper: Looper) : SimpleBasePlayer(looper) {
    override fun getState(): State {
        val track = Player.getTrack()
        return State.Builder()
            .setAvailableCommands(
                AndroidPlayer.Commands.Builder()
                    // supported inputs
                    .add(COMMAND_PLAY_PAUSE)
                    .add(COMMAND_SET_REPEAT_MODE)
                    .add(COMMAND_SET_SHUFFLE_MODE)
                    .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)

                    // required for it to show track progress
                    .add(COMMAND_GET_CURRENT_MEDIA_ITEM)
                    .add(COMMAND_GET_METADATA)

                    .build()
            )

            .setRepeatMode(
                when (Player.getRepeat()) {
                    RepeatOption.ALL -> REPEAT_MODE_ALL
                    RepeatOption.TRACK -> REPEAT_MODE_ONE
                    RepeatOption.OFF -> REPEAT_MODE_OFF
                }
            )
            .setShuffleModeEnabled(Player.getShuffle() == ShuffleOption.ON)

            // This must be populated if playbackState == STATE_READY (see below)
            .setPlaylist(track?.let { track ->
                listOf(
                    MediaItemData.Builder(track.file)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artists.joinToString(", "))
                                .setAlbumTitle(track.album)
                                .setAlbumArtist(track.metadata.firstOrNull { it?.key == "albumartist" }?.value)
                                .setDurationMs((track.durationSeconds * 1000).toLong())
                                .setArtworkData(albumArt, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                .build()
                        )
                        .build()
                )
            } ?: emptyList())

            .setCurrentMediaItemIndex(0)

            // there is no "playing" state in this interface, it counts as playing if:
            //   playbackState == STATE_READY &&
            //   playWhenReady == true &&
            //   playbackSuppressionReason == PLAYBACK_SUPPRESSION_REASON_NONE
            .setPlaybackState(if (track == null) STATE_IDLE else STATE_READY) // default: STATE_IDLE
            .setPlayWhenReady(!Player.getPaused(), PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) // default: false
            // .setPlaybackSuppressionReason(PLAYBACK_SUPPRESSION_REASON_NONE) // default

            // see: STATE_BUFFERING
            // .setIsLoading(false) // default

            .setContentPositionMs(
                if (track != null) {
                    val ms = Player.getPosition()

                    if (Player.getPaused()) PositionSupplier.getConstant(ms)
                    else PositionSupplier.getExtrapolating(ms, 1f)
                } else {
                    PositionSupplier.ZERO
                }
            )

            .setDeviceInfo(
                if (Player.isPlaybackOwner()) {
                    DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL).build()
                } else {
                    // seemingly can't prevent android from adding a volume slider due to this
                    DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE).build()
                }
            )

            .build()
    }

    // To call invalidState (which is protected) from outside this class
    fun triggerUpdate() {
        invalidateState()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) {
            Player.play()
        } else {
            Player.pause()
        }

        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        when (seekCommand) {
            COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                Player.seek(positionMs)
            }

            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                Player.next()
            }

            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                Player.previous()
            }

            // not supported
            else -> return Futures.immediateCancelledFuture<Void?>()
        }

        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        Player.setShuffle(
            when (shuffleModeEnabled) {
                true -> ShuffleOption.ON
                false -> ShuffleOption.OFF
            }
        )

        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        Player.setRepeat(
            when (repeatMode) {
                REPEAT_MODE_ALL -> RepeatOption.ALL
                REPEAT_MODE_ONE -> RepeatOption.TRACK
                else -> RepeatOption.OFF
            }
        )

        return Futures.immediateVoidFuture()
    }
}

@UnstableApi
fun initMediaControls(context: MediaSessionService): MediaSession {
    println("Initializing media controls")

    val player = PMPPlayer(Looper.getMainLooper())
    val session = MediaSession.Builder(context, player).build()

    updateMediaNotification(context, session)

    val handler = Handler(context.mainLooper)

    fun update() {
        handler.post {
            player.triggerUpdate()
            updateMediaNotification(context, session)
        }
    }

    Player.EVENT_REPEAT_CHANGED.register { update() }
    Player.EVENT_SHUFFLE_CHANGED.register { update() }
    Player.EVENT_PLAY_PAUSE.register { update() }
    Player.EVENT_SEEK.register { update() }

    Player.EVENT_TRACK_CHANGE.register { event ->
        // only send the update after the album art has loaded to allow the OS to animate the track change and the album art together
        event.picture.thenAccept {
            albumArt = it
            update()
        }
    }

    return session
}

@UnstableApi
fun updateMediaNotification(context: Context, session: MediaSession) {
    val track = Player.getTrack() ?: return

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val notification = NotificationCompat.Builder(context, "mediaNotification")
            .setOngoing(true)
            .setContentTitle(track.title)
            .setContentText(track.artists.joinToString(", "))
            .setSmallIcon(R.drawable.pmp)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
            .setSilent(true)

            // launch the app when now when the playing card it pressed
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }
}