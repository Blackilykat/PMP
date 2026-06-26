/*
 * Copyright (C) 2025 Blackilykat and contributors
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

package dev.blackilykat.pmp.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kc7bfi.jflac.FLACDecoder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.blackilykat.pmp.util.FLACUtil;
import dev.blackilykat.pmp.util.Pair;

/// A track in the library with information needed by the server.
public class Track {
	private static final Logger LOGGER = LogManager.getLogger(Track.class);

	/// The actual FLAC file location on disk.
	@JsonIgnore
	public final File file;
	/// The name of [#file], needed to expose filename without the full path to clients.
	public final String filename;
	/// Last known modification date of [#file], used to keep an up-to-date cache.
	public long lastModified;
	/// Checksum of [#file], used to allow clients to notice differences in file contents.
	public long checksum;
	/// All FLAC metadata in the file.
	public List<Pair<String, String>> metadata;

	/// Create a track with known details, used in storage deserialization.
	///
	/// This constructor trusts the input data and does not read the file at all.
	@JsonCreator
	public Track(String filename, long lastModified, long checksum, List<Pair<String, String>> metadata) {
		this.filename = filename;
		this.file = Library.LIBRARY.toPath().resolve(filename).toFile();
		this.lastModified = lastModified;
		this.checksum = checksum;
		this.metadata = metadata;
	}

	/// Create a track with unknown details by reading `file`.
	///
	/// @see #reload
	public Track(File file) throws IOException {
		this.file = file;
		this.filename = file.getName();
		reload();
	}

	/// Reload lastModified, checksum and metadata from file
	public void reload() throws IOException {
		if(!file.exists()) {
			throw new IllegalStateException("File does not exist");
		}

		lastModified = file.lastModified();

		Checksum checksum = new CRC32();
		try(CheckedInputStream is = new CheckedInputStream(new FileInputStream(file), checksum)) {
			FLACDecoder decoder = new FLACDecoder(is);

			this.metadata = FLACUtil.extractMetadata(decoder.readMetadata());

			// finish calculating the checksum
			// 0.125 MiB
			byte[] buf = new byte[0x20_000];
			while(is.available() > 0) {
				var _ = is.read(buf);
			}
		} catch(FileNotFoundException _) {
			LOGGER.error("Track#reload: file not found, this should be unreachable");
			throw new IllegalStateException("File does not exist");
		}

		this.checksum = checksum.getValue();
	}
}
