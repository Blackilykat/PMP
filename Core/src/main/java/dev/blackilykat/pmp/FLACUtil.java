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

package dev.blackilykat.pmp;

import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.metadata.VorbisString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class FLACUtil {
	private static final Logger LOGGER = LogManager.getLogger(FLACUtil.class);

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

	public static List<Pair<String, String>> extractMetadata(Metadata[] metadata) {
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
}
