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
    var audioTrack: AudioTrack? = null
    var bytesPerFrame: Int = 0

    override fun setupTrack(track: Track) {
        val info = track.playbackInfo;
        bytesPerFrame = (info.bitsPerSample / 8) * info.channels

        val format = AudioFormat.Builder()
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
                    1 -> AudioFormat.CHANNEL_OUT_MONO;
                    2 -> AudioFormat.CHANNEL_OUT_FRONT_LEFT.or(AudioFormat.CHANNEL_OUT_FRONT_RIGHT);
                    else -> 0;
                }
            )
            .build()

        println("Android backend format: $format")

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = AudioTrack.Builder()
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(AudioTrack.getMinBufferSize(format.sampleRate, format.channelMask, format.encoding))
            .build()

        audioTrack!!.play()
    }

    override fun write(pcm: ByteArray, offset: Int, length: Int) {
        // AudioTrack ignores half-frames at the end of its write call. This aligns the given parameters to include any previously discarded samples in their entirety.
        val alignedOffset = offset - (offset % bytesPerFrame)
        var alignedLength = length + (offset % bytesPerFrame)
        alignedLength -= alignedLength % bytesPerFrame
        audioTrack!!.write(pcm, alignedOffset, alignedLength)
    }

    override fun close() {
        audioTrack?.stop()
        audioTrack?.release()
    }
}