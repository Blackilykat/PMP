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

import dev.blackilykat.jpasimple.PASimple;
import dev.blackilykat.jpasimple.PulseAudioException;
import dev.blackilykat.jpasimple.SampleFormat;
import dev.blackilykat.jpasimple.SampleSpec;
import dev.blackilykat.pmp.client.Track;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;

public class PulseAudioBackend extends ConvertingAudioBackend {
	private static final int SAMPLE_RATE = 44100;
	private static final int CHANNELS = 2;
	private static final int BITS_PER_SAMPLE = 16;
	private static final boolean LITTLE_ENDIAN = true;
	private static final SampleFormat SAMPLE_FORMAT = SampleFormat.S16LE;

	private static final Logger LOGGER = LogManager.getLogger(PulseAudioBackend.class);
	protected PASimple paSimple;

	public PulseAudioBackend() {
		super(SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE, LITTLE_ENDIAN);
	}

	@Override
	public void setupTrack(Track track) throws IOException {
		super.setupTrack(track);
		if(paSimple != null) {
			try {
				paSimple.drain();
				paSimple.close();
			} catch(PulseAudioException e) {
				throw new IOException(e);
			}
		}
		Track.PlaybackInfo info = track.getPlaybackInfo();
		try {
			paSimple = new PASimple(null, "PMP", false, null, track.getTitle(),
					new SampleSpec(SAMPLE_FORMAT, SAMPLE_RATE, (short) CHANNELS), null);
		} catch(OperationNotSupportedException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void close() {
		LOGGER.warn("Closing PulseAudioBackend");
		if(paSimple != null) {
			paSimple.close();
		}
	}

	@Override
	public void writeConverted(byte[] pcm, int offset, int length) throws IOException {
		try {
			paSimple.write(pcm, offset, length);
		} catch(PulseAudioException e) {
			throw new IOException(e);
		}
	}

	@Override
	public int getLatency() {
		if(paSimple == null) {
			return 0;
		}
		return (int) paSimple.getLatency();
	}

	@Override
	public void flush() throws IOException {
		try {
			if(paSimple != null) {
				paSimple.flush();
			}
		} catch(PulseAudioException e) {
			throw new IOException(e);
		}
	}
}
