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

import dev.blackilykat.pmp.client.Main;
import dev.blackilykat.pmp.client.Track;

import java.io.IOException;

/// Abstraction layer to define audio backends to interact with the operating system and hardware.
///
/// Desktop implementations of this should generally be included in the Client subproject.
///
/// UI layers, if appropriate, are free to implement a custom audio backend.
public abstract class AudioBackend {
	/// The selected audio backend object. This is set during startup and never overridden.
	///
	/// If a UI layer wants to use a custom audio backend, it should initialize it and set this variable
	/// before calling [Main#main]. If this is not done, the logic layer will choose a compatible desktop
	/// audio backend and initialize it.
	public static AudioBackend backend = null;

	/// Do any needed setup for a track with the given stream info. Will be called for every new track loaded. May do
	/// nothing if a previous setup works for this track.
	abstract public void setupTrack(Track track) throws IOException;

	/// Write PCM data to the backend. Must block.
	///
	/// Implementations can expect pcm to be the same byte array for the same track. The array only changes between two
	/// calls when {@link #setupTrack} is called between them.
	abstract public void write(byte[] pcm, int offset, int length) throws IOException;

	/// Close the audio backend. Will not be reopened after. Will only be called in case of an error or shutdown.
	abstract public void close();

	/// Clear the backend audio buffer (if any) to avoid byte misalignment when seeking.
	public void frameAlign() throws IOException {
	}

	/// Get the latency, in milliseconds, as reported by the audio backend. If this is not supported, return 0.
	public int getLatency() {
		return 0;
	}

	/// Flush previous audio data. If this is not supported, does nothing.
	public void flush() throws IOException {
	}
}
