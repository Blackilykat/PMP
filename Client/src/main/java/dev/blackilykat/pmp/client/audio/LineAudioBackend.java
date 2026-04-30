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

import dev.blackilykat.pmp.client.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

public class LineAudioBackend extends ConvertingAudioBackend {
	private static final Logger LOGGER = LogManager.getLogger(LineAudioBackend.class);
	private static final AudioFormat PLAYBACK_AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100,
			16,
			2, 4, 44100, false);

	private SourceDataLine line;

	public LineAudioBackend() {
		super((int) PLAYBACK_AUDIO_FORMAT.getSampleRate(), PLAYBACK_AUDIO_FORMAT.getChannels(),
				PLAYBACK_AUDIO_FORMAT.getSampleSizeInBits(), !PLAYBACK_AUDIO_FORMAT.isBigEndian());
		try {
			Line.Info info = new DataLine.Info(SourceDataLine.class, PLAYBACK_AUDIO_FORMAT,
					Player.PLAYBACK_BUFFER_SIZE);
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(PLAYBACK_AUDIO_FORMAT, Player.PLAYBACK_BUFFER_SIZE);
			line.start();
		} catch(LineUnavailableException e) {
			// LineAudioBackend is the backend used if all other backends are unavailable. If this exception is
			// thrown, the program does not know how to play audio. This will get logged in the Audio thread and stop
			// it. This is the correct behavior.
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		LOGGER.warn("Closing LineAudioBackend");
		line.close();
		line = null;
	}

	@Override
	public void writeConverted(byte[] pcm, int offset, int length) throws IOException {
		line.write(pcm, offset, length);
	}
}
