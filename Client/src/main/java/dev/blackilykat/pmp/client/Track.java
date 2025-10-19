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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.blackilykat.pmp.Filter;
import dev.blackilykat.pmp.FilterOption;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.metadata.VorbisString;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	private PlaybackInfo playbackInfo;
	private File file;
	private long lastModified;
	private long checksum;
	private double durationSeconds;

	/**
	 * Creates a track based on a file. Reads the entirety of the file to derive its checksum and metadata.
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

		lastModified = file.lastModified();

		Checksum sum = new CRC32();
		CheckedInputStream is = new CheckedInputStream(new FileInputStream(file), sum);

		FLACDecoder decoder = new FLACDecoder(is);
		Metadata[] metadata = decoder.readMetadata();

		for(Metadata metadatum : metadata) {
			if(metadatum instanceof StreamInfo streamInfo) {
				this.playbackInfo = new PlaybackInfo(streamInfo);
				durationSeconds = streamInfo.getTotalSamples() / (double) streamInfo.getSampleRate();
				break;
			}
		}

		this.metadata = extractMetadata(metadata);

		while(is.available() > 0) {
			//noinspection ResultOfMethodCallIgnored
			is.read(checksumBuffer);
		}
		checksum = sum.getValue();
	}

	@JsonCreator
	private Track() {
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

	@JsonIgnore
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

	public PlaybackInfo getPlaybackInfo() {
		return playbackInfo;
	}

	@JsonIgnore
	public AudioFormat getAudioFormat() {
		return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, playbackInfo.getSampleRate(),
				playbackInfo.getBitsPerSample(), playbackInfo.getChannels(),
				playbackInfo.getBitsPerSample() * playbackInfo.getChannels() / 8,
				(float) playbackInfo.getSampleRate() / playbackInfo.getChannels(), false);
	}

	public boolean matches(FilterOption option) {
		String key = option.getParent().key;
		String value = option.value;
		if(value.equals(Filter.OPTION_EVERYTHING)) {
			return true;
		}

		boolean hasKey = false;

		for(Pair<String, String> metadatum : metadata) {
			if(metadatum.key.equalsIgnoreCase(key)) {
				hasKey = true;

				if(metadatum.value.equalsIgnoreCase(value)) {
					return true;
				}
			}
		}


		return !hasKey && value.equals(Filter.OPTION_UNKNOWN);
	}

	/**
	 * Extracts FLAC vorbis metadata from the given file
	 *
	 * @return the metadata, or null if the file is not a valid FLAC file
	 */
	public static List<Pair<String, String>> extractMetadata(File file) {
		try(InputStream is = new FileInputStream(file)) {
			FLACDecoder decoder = new FLACDecoder(is);
			return extractMetadata(decoder.readMetadata());
		} catch(IOException e) {
			return null;
		}
	}

	private static List<Pair<String, String>> extractMetadata(Metadata[] metadata) {
		List<Pair<String, String>> list = new LinkedList<>();

		// bs protected value with no getter
		Field commentsField;
		try {
			commentsField = VorbisComment.class.getDeclaredField("comments");
			commentsField.setAccessible(true);
		} catch(NoSuchFieldException e) {
			LOGGER.error("(Track#extractMetadata) This should've been unreachable", e);
			commentsField = null;
		}

		if(commentsField != null) {
			for(Metadata metadatum : metadata) {
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

					list.add(pair);
				}
			}
		}

		return list;
	}

	public static String makeFilename(List<Pair<String, String>> metadata) {
		String title = "";
		String album = "";
		List<String> artists = new LinkedList<>();

		for(Pair<String, String> metadatum : metadata) {
			if(metadatum.key.equalsIgnoreCase("title")) {
				title = metadatum.value;
			} else if(metadatum.key.equalsIgnoreCase("album")) {
				album = metadatum.value;
			} else if(metadatum.key.equalsIgnoreCase("artist")) {
				artists.add(metadatum.value);
			}
		}

		StringBuilder builder = new StringBuilder();
		builder.append(title).append("_").append(album).append("_").append(String.join("_", artists));

		StringBuilder toReturn = new StringBuilder();
		builder.chars().forEachOrdered(i -> {
			if(Character.UnicodeScript.of(i) != Character.UnicodeScript.COMMON || (i >= '0' && i <= '9')) {
				toReturn.append((char) i);
			} else {
				toReturn.append('_');
			}
		});
		toReturn.append(".flac");

		return toReturn.toString();
	}

	public static class PlaybackInfo {
		private int minBlockSize;
		private int maxBlockSize;
		private int minFrameSize;
		private int maxFrameSize;
		private int sampleRate;
		private int channels;
		private int bitsPerSample;
		private long totalSamples;

		public PlaybackInfo(StreamInfo streamInfo) {
			minBlockSize = streamInfo.getMinBlockSize();
			maxBlockSize = streamInfo.getMaxBlockSize();
			minFrameSize = streamInfo.getMinFrameSize();
			maxFrameSize = streamInfo.getMaxFrameSize();
			sampleRate = streamInfo.getSampleRate();
			channels = streamInfo.getChannels();
			bitsPerSample = streamInfo.getBitsPerSample();
			totalSamples = streamInfo.getTotalSamples();
		}

		@JsonCreator
		public PlaybackInfo(int minBlockSize, int maxBlockSize, int minFrameSize, int maxFrameSize, int sampleRate,
				int channels, int bitsPerSample, long totalSamples) {
			this.minBlockSize = minBlockSize;
			this.maxBlockSize = maxBlockSize;
			this.minFrameSize = minFrameSize;
			this.maxFrameSize = maxFrameSize;
			this.sampleRate = sampleRate;
			this.channels = channels;
			this.bitsPerSample = bitsPerSample;
			this.totalSamples = totalSamples;
		}


		public int getMinBlockSize() {
			return minBlockSize;
		}

		public int getMaxBlockSize() {
			return maxBlockSize;
		}

		public int getMinFrameSize() {
			return minFrameSize;
		}

		public int getMaxFrameSize() {
			return maxFrameSize;
		}

		public int getSampleRate() {
			return sampleRate;
		}

		public int getChannels() {
			return channels;
		}

		public int getBitsPerSample() {
			return bitsPerSample;
		}

		public long getTotalSamples() {
			return totalSamples;
		}
	}
}
