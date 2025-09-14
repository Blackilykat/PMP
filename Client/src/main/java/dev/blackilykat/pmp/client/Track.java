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

package dev.blackilykat.pmp.client;

import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.metadata.VorbisString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

public class Track {
	private static final Logger LOGGER = LogManager.getLogger(Track.class);
	private static final byte[] checksumBuffer = new byte[1048576];
	public List<Pair<String, String>> metadata;
	private File file;
	private long lastModified;
	private long checksum;
	private double durationSeconds;

	/**
	 * Creates a track based on a file. Reads the entirety of the file to derive its checksum and metadata. If that's
	 * known data, use {@link #Track(File, List, long, long)}}.
	 *
	 * @param file the track's file
	 *
	 * @throws IOException if there is an IO exception when reading the file
	 * @throws IllegalArgumentException if file doesn't exist or is a directory
	 */
	public Track(File file) throws IOException {
		this.file = file;
		if(!file.exists()) {
			throw new IllegalArgumentException("Track doesn't exist");
		}
		if(file.isDirectory()) {
			throw new IllegalArgumentException("Track is a directory");
		}

		metadata = new LinkedList<>();
		lastModified = file.lastModified();

		Checksum sum = new CRC32();
		CheckedInputStream is = new CheckedInputStream(new FileInputStream(file), sum);

		FLACDecoder decoder = new FLACDecoder(is);
		Metadata[] metadata = decoder.readMetadata();

		// bs protected value with no getter
		Field commentsField;
		try {
			commentsField = VorbisComment.class.getDeclaredField("comments");
			commentsField.setAccessible(true);
		} catch(NoSuchFieldException e) {
			LOGGER.error("This should've been unreachable", e);
			commentsField = null;
		}

		if(commentsField != null) {
			for(Metadata metadatum : metadata) {
				if(metadatum instanceof StreamInfo streamInfo) {
					durationSeconds = streamInfo.getTotalSamples() / (double) streamInfo.getSampleRate();
					continue;
				}
				if(!(metadatum instanceof VorbisComment comments)) {
					continue;
				}

				VorbisString[] strings;
				try {
					strings = (VorbisString[]) commentsField.get(comments);
				} catch(IllegalAccessException e) {
					LOGGER.error("Shouldn't have been unable to get field value, skipping rest of metadata", e);
					break;
				}

				for(VorbisString string : strings) {
					String[] parts = string.toString().split("=");
					if(parts.length < 2) {
						LOGGER.warn("Ignoring metadatum without =: {}", string.toString());
						continue;
					}
					Pair<String, String> pair = new Pair<>(parts[0],
							Arrays.stream(parts).skip(1).collect(Collectors.joining("=")));

					this.metadata.add(pair);
				}
			}
		}

		while(is.available() > 0) {
			//noinspection ResultOfMethodCallIgnored
			is.read(checksumBuffer);
		}
		checksum = sum.getValue();
	}

	public Track(File file, List<Pair<String, String>> metadata, long lastModified, long checksum) {
		if(!file.exists()) {
			throw new IllegalArgumentException("Track doesn't exist");
		}
		if(file.isDirectory()) {
			throw new IllegalArgumentException("Track is a directory");
		}

		this.file = file;
		this.metadata = metadata;
		this.lastModified = lastModified;
		this.checksum = checksum;
	}

	public File getFile() {
		return file;
	}

	public long getChecksum() {
		return checksum;
	}

	public long getLastModified() {
		return lastModified;
	}

	public String getTitle() {
		for(Pair<String, String> metadatum : metadata) {
			// assuming there is only one title
			if(metadatum.key.equalsIgnoreCase("title")) {
				return metadatum.value;
			}
		}

		return file.getName().substring(0, file.getName().length() - 4);
	}

	public double getDurationSeconds() {
		return durationSeconds;
	}
}
