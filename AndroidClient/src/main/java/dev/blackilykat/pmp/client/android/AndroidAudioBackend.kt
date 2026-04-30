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

import android.media.AudioFormat
import android.media.AudioTrack
import dev.blackilykat.pmp.client.Track
import dev.blackilykat.pmp.client.audio.AudioBackend

class AndroidAudioBackend : AudioBackend() {
    var audioTrack: AudioTrack? = null;

    override fun setupTrack(track: Track) {
        val info = track.playbackInfo;

        audioTrack?.stop()
        audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(
                        when (info.bitsPerSample) {
                            8 -> AudioFormat.ENCODING_PCM_8BIT;
                            16 -> AudioFormat.ENCODING_PCM_16BIT;
                            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED;
                            32 -> AudioFormat.ENCODING_PCM_32BIT;
                            else -> AudioFormat.ENCODING_INVALID;
                        }
                    )
                    .setSampleRate(info.sampleRate)
                    .setChannelMask(
                        when (info.channels) {
                            1 -> AudioFormat.CHANNEL_OUT_FRONT_CENTER;
                            2 -> AudioFormat.CHANNEL_OUT_FRONT_LEFT.or(AudioFormat.CHANNEL_OUT_FRONT_RIGHT);
                            else -> 0;
                        }
                    )
                    .build()
            )
            .build()
    }

    override fun write(pcm: ByteArray, offset: Int, length: Int) {
        audioTrack!!.write(pcm, offset, length)
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}