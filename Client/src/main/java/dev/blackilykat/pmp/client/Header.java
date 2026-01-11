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

package dev.blackilykat.pmp.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public class Header {
	private static final Logger LOGGER = LogManager.getLogger(Header.class);

	public final EventSource<Void> eventHeaderRemoved = new EventSource<>();
	public final EventSource<String> eventLabelChanged = new EventSource<>();
	public final EventSource<String> eventKeyChanged = new EventSource<>();

	public final int id;

	@JsonIgnore
	public Type type;
	private String label;
	private String key;

	public Header(int id, String label, String key) {
		this.id = id;
		this.label = label;
		this.key = key;

		updateType();
	}

	// When storage is loaded, the library isn't and calling #updateType will raise an exception. This constructor
	// allows the parser to create a "typeless" header, which will be updated when the library is loaded.
	@JsonCreator
	private Header(int id, String key) {
		this.id = id;
		this.key = key;
		this.type = Type.STRING;
	}

	public void updateType() {
		Type type = Type.INTEGER;
		if(key.equalsIgnoreCase("duration")) {
			type = Type.DURATION;
		} else if(key.equalsIgnoreCase("title")) {
			type = Type.TITLE;
		} else if(key.equalsIgnoreCase("tracknumber")) {
			type = Type.TRACKNUMBER;
		} else {
			for(Track track : ClientStorage.MAIN.tracks.values()) {
				for(Pair<String, String> metadatum : track.metadata) {
					if(!metadatum.key.equalsIgnoreCase(key)) {
						continue;
					}

					if(type == Type.INTEGER) {
						try {
							Integer.parseInt(metadatum.value);
						} catch(NumberFormatException e) {
							try {
								Double.valueOf(metadatum.value);
								type = Type.DOUBLE;
							} catch(NumberFormatException ex) {
								type = Type.STRING;
							}
						}
					} else if(type == Type.DOUBLE) {
						try {
							Double.parseDouble(metadatum.value);
						} catch(NumberFormatException e) {
							type = Type.STRING;
							break;
						}
					}
				}
			}
		}
		this.type = type;

		LOGGER.debug("{} is of type {}", this, type);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
		eventLabelChanged.call(label);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
		updateType();
		eventKeyChanged.call(key);
	}

	@JsonIgnore
	public String getStringValue(Track track) {
		if(type == Type.TITLE) {
			return track.getTitle();
		}

		if(type == Type.STRING) {
			List<String> values = new LinkedList<>();
			for(Pair<String, String> metadatum : track.metadata) {
				if(!metadatum.key.equalsIgnoreCase(key)) {
					continue;
				}
				values.add(metadatum.value);
			}
			return String.join(", ", values);
		}

		if(type == Type.INTEGER || type == Type.DOUBLE || type == Type.TRACKNUMBER) {
			for(Pair<String, String> metadatum : track.metadata) {
				if(!metadatum.key.equalsIgnoreCase(key)) {
					continue;
				}
				return metadatum.value;
			}
			return "";
		}

		if(type == Type.DURATION) {
			int seconds = (int) track.getDurationSeconds();
			return String.format("%d:%02d", seconds / 60, seconds % 60);
		}

		LOGGER.error("Header#getStringValue: Unknown header type {}: this should be unreachable", type);
		return "INVALID HEADER";
	}

	public int compare(Track a, Track b) {
		switch(type) {
			case TITLE -> {
				return a.getTitle().compareToIgnoreCase(b.getTitle());
			}
			case INTEGER, DOUBLE -> {
				double valueA = 0;
				boolean foundA = false;
				for(Pair<String, String> metadatum : a.metadata) {
					if(!metadatum.key.equalsIgnoreCase(key)) {
						continue;
					}
					valueA = Double.parseDouble(metadatum.value);
					foundA = true;
					break;
				}
				if(!foundA) {
					return -1;
				}

				double valueB = 0;
				boolean foundB = false;
				for(Pair<String, String> metadatum : a.metadata) {
					if(!metadatum.key.equalsIgnoreCase(key)) {
						continue;
					}
					valueB = Double.parseDouble(metadatum.value);
					foundB = true;
					break;
				}
				if(!foundB) {
					return 1;
				}

				return Double.compare(valueA, valueB);
			}
			case STRING -> {
				return getStringValue(a).compareToIgnoreCase(getStringValue(b));
			}
			case DURATION -> {
				return Double.compare(a.getDurationSeconds(), b.getDurationSeconds());
			}
			case TRACKNUMBER -> {
				String albumA = null;
				Integer tracknumberA = null;

				for(Pair<String, String> metadatum : a.metadata) {
					if(albumA != null && tracknumberA != null) {
						break;
					}

					if(metadatum.key.equalsIgnoreCase("album")) {
						albumA = metadatum.value;
					}
					if(metadatum.key.equalsIgnoreCase("tracknumber")) {
						tracknumberA = Integer.valueOf(metadatum.value);
					}
				}


				String albumB = null;
				Integer tracknumberB = null;

				for(Pair<String, String> metadatum : b.metadata) {
					if(albumB != null && tracknumberB != null) {
						break;
					}

					if(metadatum.key.equalsIgnoreCase("album")) {
						albumB = metadatum.value;
					}
					if(metadatum.key.equalsIgnoreCase("tracknumber")) {
						tracknumberB = Integer.valueOf(metadatum.value);
					}
				}
				if(albumA == null && albumB == null) {
					return 0;
				}
				if(albumB == null) {
					return 1;
				}
				if(albumA == null) {
					return -1;
				}

				if(albumA.equals(albumB)) {
					if(tracknumberA == null && tracknumberB == null) {
						return 0;
					}
					if(tracknumberA == null) {
						return -1;
					}
					if(tracknumberB == null) {
						return 1;
					}
					return Integer.compare(tracknumberA, tracknumberB);
				}

				return albumA.compareToIgnoreCase(albumB);
			}
		}

		LOGGER.error("Header#compare: Unknown header type {}: this should be unreachable", type);
		return 0;
	}

	@Override
	public String toString() {
		return "Header#" + id + "(" + key + "," + label + ")";
	}

	public enum Type {
		/**
		 * Title of the track, as obtained in {@link Track#getTitle()}. Sorting happens alphabetically.
		 * <p>This type is specific to the "title" metadata key.
		 */
		TITLE,
		/**
		 * One or multiple string values, separated by commas. Sorting happens alphabetically on the comma-separated
		 * resulting string.
		 */
		STRING,
		/**
		 * One integer value. Sorting happens numerically, with empty values being "greater" than everything else.
		 * Multiple values on one track means only one is taken into account.
		 */
		INTEGER,
		/**
		 * One double value. Sorting happens numerically, with empty values being "greater" than everything else.
		 * Multiple values on one track means only one is taken into account.
		 */
		DOUBLE,
		/**
		 * The duration of the track, formated as %d:%02d (m:ss). Obtained from the streaminfo header of the track.
		 * <p>This type is specific to the "duration" metadata key.
		 */
		DURATION,
		/**
		 * Like {@link #INTEGER}, but it then sorts alphabetically by the "album" metadata key. Multiple values on one
		 * track means only one is taken into account (both for album and tracknumber).
		 * <p>This type is specific to the "tracknumber" metadata key.
		 */
		TRACKNUMBER
	}
}
