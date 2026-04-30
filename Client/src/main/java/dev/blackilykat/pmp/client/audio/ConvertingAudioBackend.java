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

package dev.blackilykat.pmp.client.audio;

import dev.blackilykat.pmp.client.Track;
import dev.blackilykat.pmp.util.ConcatenatedInputStream;
import dev.blackilykat.pmp.util.ExposedByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Audio backend that converts audio to a single PCM format. Does not use javax.sound.sampled packages, so this can be
/// used in environments where they are not available (i.e. Android)
///
/// Converts between different sample rates by interpolating between two samples.
///
/// Can perform conversions from and to:
/// - any sample rate
/// - 8, 16, 24, 32 bits per sample
/// - 1 and 2 channels
/// - little endian and big endian (though input is assumed to be little endian due to that being the format in FLAC)
public abstract class ConvertingAudioBackend extends AudioBackend {
	private static final Logger LOGGER = LogManager.getLogger(ConvertingAudioBackend.class);
	public final int sampleRate;
	public final int channels;
	public final int bitsPerSample;
	public final boolean littleEndian;
	protected Track.PlaybackInfo info;
	private int lastLeftSample;
	private int lastRightSample;
	private int currentLeftSample;
	private int currentRightSample;
	/**
	 * Value that represents how far from the last OG sample the next converted sample is in the time domain, measured
	 * in OG samples.
	 */
	private double position;
	private ExposedByteArrayOutputStream convertedStream = new ExposedByteArrayOutputStream();
	private InputStream lastStream = null;


	public ConvertingAudioBackend(int sampleRate, int channels, int bitsPerSample, boolean littleEndian) {
		if(channels < 1 || channels > 2) {
			throw new IllegalArgumentException("Unsupported channel amount (" + channels + ")");
		}
		if(bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24 && bitsPerSample != 32) {
			throw new IllegalArgumentException("Unsupported bits per sample (" + bitsPerSample + ")");
		}

		this.sampleRate = sampleRate;
		this.channels = channels;
		this.bitsPerSample = bitsPerSample;
		this.littleEndian = littleEndian;
	}

	@Override
	public void setupTrack(Track track) throws IOException {
		info = track.getPlaybackInfo();
		// clear leftover bytes from the last track
		lastStream = null;
	}

	@Override
	public void write(byte[] pcm, int offset, int length) throws IOException {
		assert info != null;

		convertedStream.clear();

		InputStream thisStream = new ByteArrayInputStream(pcm, offset, length);
		// some samples may have been cut off mid-sample in the last stream
		InputStream is = lastStream == null ? thisStream : new ConcatenatedInputStream(lastStream, thisStream);

		int shift = bitsPerSample - info.getBitsPerSample();
		while(is.available() > info.getBitsPerSample() * info.getChannels() / 8) {
			if(position > 1) {
				advanceOneFrame(is);
				continue;
			}

			int newLeftSample = (int) (lastLeftSample * (1 - position) + currentLeftSample * position);

			int newRightSample = 0;
			if(info.getChannels() == 2) {
				newRightSample = (int) (lastRightSample * (1 - position) + currentRightSample * position);
			}

			if(shift > 0) {
				newLeftSample <<= shift;
				newRightSample <<= shift;
			} else if(shift < 0) {
				newLeftSample >>= -shift;
				newRightSample >>= -shift;
			}

			if(channels == 1) {
				if(info.getChannels() == 2) {
					newLeftSample = (int) (((long) newLeftSample + newRightSample) / 2);
				}

				writeSample(convertedStream, newLeftSample, bitsPerSample, littleEndian);
			} else {
				assert channels == 2;
				if(info.getChannels() == 1) {
					newRightSample = newLeftSample;
				}

				writeSample(convertedStream, newLeftSample, bitsPerSample, littleEndian);
				writeSample(convertedStream, newRightSample, bitsPerSample, littleEndian);
			}


			position += (double) info.getSampleRate() / sampleRate;
		}

		writeConverted(convertedStream.getBackingArray(), 0, convertedStream.size());

		lastStream = is.available() == 0 ? null : thisStream;
	}

	@Override
	public abstract void close();

	@Override
	public void frameAlign() {
		lastStream = null;
		currentLeftSample = 0;
		currentRightSample = 0;
	}

	/**
	 * Write the converted PCM audio stream in the requested format. Data in the given array will be overridden soon
	 * after this method returns, so you must either use the data immediately or copy it for later use. The data always
	 * contains full frames, there is no situation where it may contain half a sample in one call and the other in the
	 * call after. Different channels in the same frame are always grouped in the same method call.
	 */
	public abstract void writeConverted(byte[] pcm, int offset, int length) throws IOException;

	private void advanceOneFrame(InputStream is) throws IOException {
		lastLeftSample = currentLeftSample;
		lastRightSample = currentRightSample;
		position -= 1;
		currentLeftSample = readSample(is, info.getBitsPerSample(), true);
		if(info.getChannels() == 2) {
			currentRightSample = readSample(is, info.getBitsPerSample(), true);
		}
	}

	private static int readSample(InputStream is, int bitsPerSample, boolean littleEndian) throws IOException {
		int bytesPerSample = bitsPerSample / 8;
		int val = 0;
		if(littleEndian) {
			for(int i = 0; i < bytesPerSample; i++) {
				val += is.read() << (i * 8);
			}
		} else {
			for(int i = 0; i < bytesPerSample; i++) {
				val <<= 8;
				val += is.read();
			}
		}

		if(bitsPerSample != 32 && val >= 0x80 << (bitsPerSample - 8)) {
			val -= (1 << (bitsPerSample));
		}

		return val;
	}

	private static void writeSample(OutputStream os, int value, int bitsPerSample, boolean littleEndian)
			throws IOException {
		int bytesPerSample = bitsPerSample / 8;

		if(bitsPerSample != 32 && value < 0) {
			value += (1 << (bitsPerSample));
		}

		if(littleEndian) {
			for(int i = 0; i < bytesPerSample; i++) {
				os.write(value & 0xFF);
				value >>= 8;
			}
		} else {
			for(int i = bytesPerSample - 1; i >= 0; i--) {
				os.write((value >> (i * 8)) & 0xFF);
			}
		}
	}
}
