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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kc7bfi.jflac.FLACDecoder;

import dev.blackilykat.pmp.Action;
import dev.blackilykat.pmp.FilterInfo;
import dev.blackilykat.pmp.Globals;
import dev.blackilykat.pmp.Order;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.event.RetroactiveEventSource;
import dev.blackilykat.pmp.messages.FilterListMessage;
import dev.blackilykat.pmp.messages.PlaybackControlMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;
import dev.blackilykat.pmp.util.FLACUtil;
import dev.blackilykat.pmp.util.Pair;
import dev.blackilykat.pmp.util.ScopedValue;

/// General management of loading, updating, sorting, filtering the library.
///
/// @see #EVENT_SELECTED_TRACKS_UPDATED
public class Library {
	/// Emitted when the library is done loading.
	///
	/// Contains the list of all tracks in the library.
	public static final RetroactiveEventSource<Collection<Track>> EVENT_LOADED = new RetroactiveEventSource<>();
	/// Emitted when a filter is added either locally by the user or remotely through another device.
	///
	/// Contains the added filter.
	public static final EventSource<Filter> EVENT_FILTER_ADDED = new EventSource<>();
	/// Emitted when a filter is removed either locally by the user or remotely through another device.
	///
	/// Contains the removed filter.
	public static final EventSource<Filter> EVENT_FILTER_REMOVED = new EventSource<>();
	/// Emitted when a filter was added, removed or moved.
	///
	/// Contains the new list of filters.
	public static final EventSource<List<Filter>> EVENT_FILTERS_UPDATED = new EventSource<>();
	/// Emitted when a track is added to the library.
	///
	/// Contains the new track object.
	///
	/// Not suitable for updating UI track list as it does not account for sorting or filters.
	///
	/// @see #EVENT_SELECTED_TRACKS_UPDATED
	public static final EventSource<Track> EVENT_TRACK_ADDED = new EventSource<>();
	/// Emitted when a track is removed from the library.
	///
	/// Contains the old track object.
	///
	/// Not suitable for updating UI track list as it does not account for sorting or filters.
	///
	/// @see #EVENT_SELECTED_TRACKS_UPDATED
	public static final EventSource<Track> EVENT_TRACK_REMOVED = new EventSource<>();
	/// Emitted when a header is added, removed or moved.
	///
	/// Contains the new list of headers.
	public static final EventSource<List<Header>> EVENT_HEADERS_UPDATED = new RetroactiveEventSource<>();
	/// Emitted when a header is added.
	///
	/// Contains the new header, which is always added as the last header.
	public static final EventSource<Header> EVENT_HEADER_ADDED = new EventSource<>();
	/// Emitted when a header is removed.
	///
	/// Contains the old header.
	public static final EventSource<Header> EVENT_HEADER_REMOVED = new EventSource<>();
	/// Emitted when a header is moved to a different position.
	///
	/// Moving a header only affects UI order.
	public static final EventSource<HeaderMovedEvent> EVENT_HEADER_MOVED = new EventSource<>();
	/// Emitted when a filter is moved to a different position.
	///
	/// Moving a filter affects both UI and logic, as filters are applied in order.
	public static final EventSource<FilterMovedEvent> EVENT_FILTER_MOVED = new EventSource<>();
	/// Emitted when the list of filtered and sorted tracks is updated.
	///
	/// This event is suitable to update a tracklist in the UI layer. Tracks must be displayed in the order
	/// they appear in the given list, and there must not be any missing or extra tracks.
	public static final EventSource<SelectedTracksUpdatedEvent> EVENT_SELECTED_TRACKS_UPDATED = new EventSource<>();
	/// Emitted when the sorting is updated.
	public static final EventSource<SortingHeaderUpdatedEvent> EVENT_SORTING_HEADER_UPDATED = new EventSource<>();

	/// True when [#reloadSelection] should be prevented from running for performance reasons.
	///
	/// Used in [#collectReloads].
	private static final ScopedValue<Boolean> NO_RELOAD_SELECTION = ScopedValue.newInstance();
	/// Used to prevent recursion when handling filter option updates.
	private static final ScopedValue<Boolean> IGNORE_FILTER_OPTION_UPDATE = ScopedValue.newInstance();

	private static final Logger LOGGER = LogManager.getLogger(Library.class);
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

	/// The directory containing the library.
	private static File library = null;
	/// The current list of filtered and sorted tracks.
	///
	/// @see #EVENT_SELECTED_TRACKS_UPDATED
	private static List<Track> selectedTracks = new LinkedList<>();
	private static Header sortingHeader = null;
	private static Order sortingOrder = Order.ASCENDING;

	/// Reloads the selection if [#NO_RELOAD_SELECTION] does not prevent it from doing so.
	///
	/// Applies filters and filter options and sorts the filtered list.
	///
	/// @see #EVENT_SELECTED_TRACKS_UPDATED
	public static void reloadSelection() {
		if(NO_RELOAD_SELECTION.orElse(false)) {
			return;
		}
		LOGGER.debug("Reloading selection");

		Set<Track> selection = new HashSet<>(ClientStorage.MAIN.tracks.values());
		Set<Track> newSelection = new HashSet<>();

		for(Filter filter : ClientStorage.MAIN.filters.get()) {
			Set<String> foundValues = new HashSet<>();
			foundValues.add(Filter.OPTION_EVERYTHING);
			boolean anyUnknowns = false;
			for(Track track : selection) {
				boolean unknown = true;
				for(Pair<String, String> metadatum : track.metadata) {
					if(metadatum.key.equalsIgnoreCase(filter.key)) {
						unknown = false;
						foundValues.add(metadatum.value);
					}
				}
				if(unknown) {
					anyUnknowns = true;
				}
			}
			if(anyUnknowns) {
				foundValues.add(Filter.OPTION_UNKNOWN);
			}

			filter.applyOptionValues(new ArrayList<>(foundValues));

			boolean anyNegative = false;
			boolean anyPositive = false;
			FilterOption everything = null;
			for(FilterOption option : filter.getOptions()) {
				FilterOption.State state = option.getState();
				if(option.value.equals(Filter.OPTION_EVERYTHING)) {
					everything = option;
				}

				if(state == FilterOption.State.NONE) {
					continue;
				}
				if(state == FilterOption.State.NEGATIVE) {
					anyNegative = true;
					continue;
				}
				anyPositive = true;

				for(Track track : selection) {
					if(track.matches(option)) {
						newSelection.add(track);
					}
				}
			}

			if(!anyPositive) {
				final FilterOption fEverything = everything;
				ScopedValue.where(NO_RELOAD_SELECTION, true).run(() -> {
					fEverything.setState(FilterOption.State.POSITIVE);
				});
				newSelection.addAll(selection);
			}

			if(anyNegative) {
				for(FilterOption option : filter.getOptions()) {
					if(option.getState() != FilterOption.State.NEGATIVE) {
						continue;
					}

					newSelection.removeIf(track -> track.matches(option));
				}
			}
			selection = newSelection;
			newSelection = new HashSet<>();
		}

		List<Track> oldSelectedTracks = selectedTracks;
		selectedTracks = selection.stream().sorted((a, b) -> {
			if(sortingHeader == null || sortingOrder == null) {
				return 0;
			}
			int multiplier = sortingOrder == Order.ASCENDING ? 1 : -1;
			int comp = sortingHeader.compare(a, b);
			if(comp == 0){
				comp = a.getFile().getName().compareToIgnoreCase(b.getFile().getName());
			}
			return comp * multiplier;
		}).collect(Collectors.toList());
		EVENT_SELECTED_TRACKS_UPDATED.call(
				new SelectedTracksUpdatedEvent(Collections.unmodifiableList(oldSelectedTracks),
						Collections.unmodifiableList(selectedTracks)));
	}

	/// Add a new filter and send the update to the server if possible.
	///
	/// @see #EVENT_FILTER_ADDED
	/// @see #EVENT_FILTERS_UPDATED
	public static void addFilter(Filter filter) {
		LOGGER.info("Adding filter {}", filter);
		ClientStorage.MAIN.filters.add(filter);

		collectReloads(() -> {
			FilterOption opt = new FilterOption(Filter.OPTION_EVERYTHING);
			filter.addOption(opt);
			opt.setState(FilterOption.State.POSITIVE);

			reloadSelection();
		});

		EVENT_FILTER_ADDED.call(filter);
		EVENT_FILTERS_UPDATED.call(ClientStorage.MAIN.filters.get());

		if(Server.isLoggedIn()) {
			Server.send(new FilterListMessage(exportFilters()));
		}
	}

	/// Remove an existing filter and send the update to the server if possible.
	///
	/// @see #EVENT_FILTER_REMOVED
	/// @see #EVENT_FILTERS_UPDATED
	public static void removeFilter(Filter filter) {
		LOGGER.info("Removing filter {}", filter);
		ClientStorage.MAIN.filters.remove(filter);

		reloadSelection();

		EVENT_FILTER_REMOVED.call(filter);
		EVENT_FILTERS_UPDATED.call(ClientStorage.MAIN.filters.get());

		if(Server.isLoggedIn()) {
			Server.send(new FilterListMessage(exportFilters()));
		}
	}

	/// Move an existing filter to a new position and send the update to the server if possible.
	///
	/// @throws IndexOutOfBoundsException if `position` is out of bounds
	/// @throws IllegalArgumentException if `filter` is not an existing filter
	///
	/// @see #EVENT_FILTER_MOVED
	/// @see #EVENT_FILTERS_UPDATED
	public static void moveFilter(Filter filter, int position) {
		if(position < 0 || position >= ClientStorage.MAIN.filters.size()) {
			throw new IndexOutOfBoundsException(
					"Position " + position + " is out of bounds for size " + ClientStorage.MAIN.filters.size());
		}

		int oldPos = ClientStorage.MAIN.filters.indexOf(filter);
		if(oldPos == -1) {
			throw new IllegalArgumentException("Filter " + filter + " is not in filters");
		}

		LOGGER.info("Moving filter {} from {} to {}", filter, oldPos, position);

		ClientStorage.MAIN.filters.remove(filter);
		ClientStorage.MAIN.filters.add(position, filter);

		reloadSelection();

		EVENT_FILTER_MOVED.call(new FilterMovedEvent(filter, position));
		EVENT_FILTERS_UPDATED.call(ClientStorage.MAIN.filters.get());

		if(Server.isLoggedIn()) {
			Server.send(new FilterListMessage(exportFilters()));
		}
	}

	/// Import filters from a network-friendly list sent by the server.
	///
	/// @see #EVENT_FILTER_ADDED
	/// @see #EVENT_FILTER_REMOVED
	/// @see #EVENT_FILTER_MOVED
	/// @see #EVENT_FILTERS_UPDATED
	public static void importFilters(@Nonnull List<FilterInfo> filterInfos) {
		if(filterInfos == null) {
			LOGGER.warn("Attempted import of null filters");
			return;
		}
		List<Filter> newFilters = new LinkedList<>();
		infoLoop:
		for(FilterInfo filterInfo : filterInfos) {
			for(Filter filter : ClientStorage.MAIN.filters.get()) {
				if(filterInfo.id() == filter.id) {
					filter.key = filterInfo.key();
					newFilters.add(filter);

					continue infoLoop;
				}
			}
			newFilters.add(new Filter(filterInfo.id(), filterInfo.key()));
		}

		for(Filter filter : ClientStorage.MAIN.filters.get()) {
			if(!newFilters.contains(filter)) {
				EVENT_FILTER_REMOVED.call(filter);
			}
		}

		for(Filter filter : newFilters) {
			if(!ClientStorage.MAIN.filters.contains(filter)) {
				EVENT_FILTER_ADDED.call(filter);
			}
		}

		int i = -1;
		for(Filter filter : newFilters) {
			i++;

			EVENT_FILTER_MOVED.call(new FilterMovedEvent(filter, i));
		}
		ClientStorage.MAIN.filters.set(newFilters);

		EVENT_FILTERS_UPDATED.call(newFilters);

		ScopedValue.where(Player.DONT_SEND_UPDATES, true).run(() -> {
			reloadSelection();
		});
	}

	/// Export filters to a network-friendly list to be sent to the server.
	public static List<FilterInfo> exportFilters() {
		List<FilterInfo> toReturn = new LinkedList<>();
		for(Filter filter : ClientStorage.MAIN.filters.get()) {
			toReturn.add(new FilterInfo(filter.id, filter.key));
		}
		return toReturn;
	}

	/// Add a new header.
	///
	/// @see #EVENT_HEADER_ADDED
	/// @see #EVENT_HEADERS_UPDATED
	public static void addHeader(Header header) {
		LOGGER.info("Adding header {}", header);

		ClientStorage.MAIN.headers.add(header);

		EVENT_HEADER_ADDED.call(header);
		EVENT_HEADERS_UPDATED.call(ClientStorage.MAIN.headers.get());
	}

	/// Move an existing header to a new position.
	///
	/// @throws IllegalArgumentException if `header` is not an existing header
	/// @throws IndexOutOfBoundsException if `position` is out of bounds
	/// @see #EVENT_HEADER_MOVED
	/// @see #EVENT_HEADERS_UPDATED
	public static void moveHeader(Header header, int position) {
		if(!ClientStorage.MAIN.headers.contains(header)) {
			throw new IllegalArgumentException(header + " is not in headers");
		}
		if(position < 0 || position >= ClientStorage.MAIN.headers.size()) {
			throw new IndexOutOfBoundsException(
					"Position " + position + " out of bounds for " + ClientStorage.MAIN.headers.size() + " headers");
		}

		LOGGER.debug("Moving {} to position {}", header, position);

		int oldPosition = ClientStorage.MAIN.headers.indexOf(header);

		ClientStorage.MAIN.headers.remove(header);

		ClientStorage.MAIN.headers.add(position, header);

		EVENT_HEADER_MOVED.call(new HeaderMovedEvent(header, oldPosition, position));
		EVENT_HEADERS_UPDATED.call(ClientStorage.MAIN.headers.get());
	}

	/// Remove an existing header.
	///
	/// @throws IllegalStateException if the header is the only header
	/// @see #EVENT_HEADER_REMOVED
	/// @see Header#eventHeaderRemoved
	/// @see #EVENT_HEADERS_UPDATED
	public static void removeHeader(Header header) {
		if(ClientStorage.MAIN.headers.size() == 1) {
			throw new IllegalStateException("Can't remove the only header");
		}

		LOGGER.info("Removing header {}", header);

		if(header == sortingHeader) {
			sortingHeader = ClientStorage.MAIN.headers.getFirst();
			sortingOrder = Order.ASCENDING;
		}

		ClientStorage.MAIN.headers.remove(header);

		EVENT_HEADER_REMOVED.call(header);
		header.eventHeaderRemoved.call(null);
		EVENT_HEADERS_UPDATED.call(ClientStorage.MAIN.headers.get());
	}

	/// Update the sorting header and order.
	///
	/// @see Header
	/// @see #EVENT_SORTING_HEADER_UPDATED
	public static void setSorting(Header header, Order order) {
		sortingHeader = header;
		sortingOrder = order;

		reloadSelection();
		EVENT_SORTING_HEADER_UPDATED.call(new SortingHeaderUpdatedEvent(header, order));
	}

	public static Header getSortingHeader() {
		return sortingHeader;
	}

	public static Order getSortingOrder() {
		return sortingOrder;
	}

	/// Get an unmodifiable view of the current list of filtered and sorted tracks.
	public static List<Track> getSelectedTracks() {
		return Collections.unmodifiableList(selectedTracks);
	}

	/// Initialize the state of the library:
	/// - Creates a library directory if it did not exist;
	/// - Loads all files in library, using the cache at [ClientStorage.Main#tracks] when possible;
	/// - Creates default headers if needed;
	/// - Updates header types (see [Header#updateType];
	/// - Creates default filters if needed;
	/// - Loads initial state of selected tracks (see [#reloadSelection]);
	/// - Registers needed listeners.
	///
	/// @throws IllegalStateException if called twice
	/// @see #EVENT_LOADED
	public static void init() {
		synchronized(INITIALIZED) {
			if(INITIALIZED.get()) {
				throw new IllegalStateException("Already initialized");
			}
			INITIALIZED.set(true);
			LOGGER.info("Initializing library");

			library = Globals.library;
			LOGGER.debug("Attempting to load library at {}", library.getAbsolutePath());
			if(!library.exists()) {
				var _ = library.mkdirs();
			}
			if(!library.isDirectory()) {
				LOGGER.error("Library is a file");
				throw new IllegalStateException("Library is a file");
			}

			File[] children = library.listFiles();
			int totalCached = 0;
			if(children != null) {
				for(File file : children) {
					if(!ClientStorage.MAIN.tracks.containsKey(file.getName())) {
						LOGGER.warn("Track {} was not cached", file.getName());
						try {
							Track track = new Track(file);
							ClientStorage.MAIN.tracks.put(file.getName(), track);
						} catch(IOException e) {
							LOGGER.error("Error loading {}", file.getName(), e);
						}
					} else if(ClientStorage.MAIN.tracks.get(file.getName()).getLastModified() != file.lastModified()) {
						LOGGER.warn("Track {} was modified after it was cached", file.getName());
						try {
							Track track = new Track(file);
							ClientStorage.MAIN.tracks.remove(file.getName());
							ClientStorage.MAIN.tracks.put(file.getName(), track);
						} catch(IOException e) {
							LOGGER.error("Error reloading {}", file.getName(), e);
						}
					} else {
						totalCached++;
					}
				}

				for(Track track : ClientStorage.MAIN.tracks.values().toArray(new Track[0])) {
					if(!track.getFile().exists()) {
						LOGGER.warn("Cached track {} no longer exists", track.getFile().getName());
						ClientStorage.MAIN.tracks.remove(track.getFile().getName());
					}
				}
			}


			LOGGER.info("{} tracks cached", totalCached);


			if(ClientStorage.MAIN.headers.empty()) {
				ClientStorage.MAIN.headers.add(new Header(0, "N°", "tracknumber"));
				ClientStorage.MAIN.headers.add(new Header(1, "Title", "title"));
				ClientStorage.MAIN.headers.add(new Header(2, "Artist", "artist"));
				ClientStorage.MAIN.headers.add(new Header(3, "Duration", "duration"));
				ClientStorage.MAIN.currentHeaderID.set(4);
			} else {
				for(Header header : ClientStorage.MAIN.headers.get()) {
					header.updateType();
				}
			}
			LOGGER.debug("Headers: {}", ClientStorage.MAIN.headers.get());

			sortingHeader = ClientStorage.MAIN.headers.getFirst();
			sortingOrder = Order.ASCENDING;

			Filter.EVENT_OPTION_CHANGED_STATE.register(evt -> {
				if(IGNORE_FILTER_OPTION_UPDATE.orElse(false)) {
					return;
				}
				if(Player.shouldSendControl()) {
					PlaybackControlMessage msg = new PlaybackControlMessage();
					msg.positiveOptions = exportPositiveOptions();
					msg.negativeOptions = exportNegativeOptions();

					if(evt.newState() == FilterOption.State.POSITIVE) {
						if(evt.option().value.equals(Filter.OPTION_EVERYTHING)) {
							msg.positiveOptions.removeIf(pair -> pair.key == evt.filter().id && !pair.value.equals(
									Filter.OPTION_EVERYTHING));
						} else {
							msg.positiveOptions.removeIf(
									pair -> pair.key == evt.filter().id && pair.value.equals(Filter.OPTION_EVERYTHING));
						}
					}

					Server.send(msg);
					ScopedValue.where(IGNORE_FILTER_OPTION_UPDATE, true).run(() -> {
						evt.option().setState(evt.oldState());
					});
					return;
				}

				Player.takeOwnershipIfNeeded();


				ScopedValue.where(Player.DONT_SEND_UPDATES, true).run(() -> {
					collectReloads(() -> {
						Filter filter = evt.filter();
						FilterOption option = evt.option();
						FilterOption.State oldState = evt.oldState();
						FilterOption.State state = evt.newState();

						if(filter != null) {
							if(option.value.equals(Filter.OPTION_EVERYTHING)) {
								if(state == FilterOption.State.POSITIVE) {
									for(FilterOption otherOption : filter.getOptions()) {
										if(otherOption != option
												&& otherOption.getState() == FilterOption.State.POSITIVE) {
											otherOption.setState(FilterOption.State.NONE);
										}
									}
								} else if(state == FilterOption.State.NONE) {
									boolean otherPositive = false;

									for(FilterOption otherOption : filter.getOptions()) {
										if(otherOption != option
												&& otherOption.getState() == FilterOption.State.POSITIVE) {
											otherPositive = true;
											break;
										}
									}

									if(!otherPositive) {
										option.setState(FilterOption.State.POSITIVE);
									}
								} else if(state == FilterOption.State.NEGATIVE) {
									option.setState(oldState);
								}
							} else {
								if(state == FilterOption.State.POSITIVE) {
									for(FilterOption otherOption : filter.getOptions()) {
										if(!otherOption.value.equals(Filter.OPTION_EVERYTHING)) {
											continue;
										}

										if(option.getState() == FilterOption.State.POSITIVE) {
											otherOption.setState(FilterOption.State.NONE);
										}
										break;
									}
								} else if(state == FilterOption.State.NONE) {
									boolean anyPositive = false;
									FilterOption everything = null;
									for(FilterOption otherOption : filter.getOptions()) {
										if(otherOption.value.equals(Filter.OPTION_EVERYTHING)) {
											everything = otherOption;
										}

										if(otherOption.getState() == FilterOption.State.POSITIVE) {
											anyPositive = true;
											break;
										}
									}

									if(!anyPositive) {
										assert everything != null;

										everything.setState(FilterOption.State.POSITIVE);
									}
								}
							}
						}
					});

					reloadSelection();
				});

				if(Player.shouldSendUpdate()) {
					PlaybackUpdateMessage msg = new PlaybackUpdateMessage();
					msg.positiveOptions = exportPositiveOptions();
					msg.negativeOptions = exportNegativeOptions();
					Server.send(msg);
				}
			});

			{
				if(ClientStorage.MAIN.filters.empty()) {
					addFilter(new Filter("artist"));
					addFilter(new Filter("album"));
				}

				collectReloads(() -> {
					for(Filter filter : ClientStorage.MAIN.filters.get()) {
						FilterOption opt = new FilterOption(Filter.OPTION_EVERYTHING);
						filter.addOption(opt);
						opt.setState(FilterOption.State.POSITIVE);
					}
					reloadSelection();
				});
			}

			EVENT_LOADED.call(ClientStorage.MAIN.tracks.values());
			EVENT_HEADERS_UPDATED.call(ClientStorage.MAIN.headers.get());

			LOGGER.info("Initialized library");
		}
	}

	/// Add a track to the library using its file.
	///
	/// Confirms it is a valid track by parsing its metadata, then uses it to create a filename (see [Track#makeFilename]).
	///
	/// Attempts to create a hard link if possible to reduce disk space waste. If that fails, it will create a copy.
	///
	/// @see #EVENT_TRACK_ADDED
	/// @throws IllegalArgumentException if the file does not exist or is a directory
	/// @throws FileAlreadyExistsException if the target file (a track with the same title, artist and album) already exists
	/// @return true if the track was added, false if it was not a valid FLAC file.
	public static boolean addTrack(File file) throws IOException {
		if(file == null || !file.exists()) {
			throw new IllegalArgumentException("File does not exist");
		}
		if(file.isDirectory()) {
			throw new IllegalArgumentException("File is a directory");
		}
		List<Pair<String, String>> metadata = FLACUtil.extractMetadata(file);
		if(metadata == null) {
			return false;
		}

		Path target = library.toPath().resolve(Track.makeFilename(metadata));
		if(target.toFile().exists()) {
			throw new FileAlreadyExistsException(target + " already exists");
		}

		try {
			Files.createLink(target, file.toPath());
			LOGGER.debug("Linked {} to {}", file, target);
		} catch(IOException e) {
			LOGGER.warn("Failed linking {} to {}", file, target, e);
			Files.copy(file.toPath(), target);
			LOGGER.debug("Copied {} to {}", file, target);
		}

		registerNewTrack(target.toFile());
		return true;
	}

	/// Add a track to the library using an arbitrary InputStream.
	///
	/// Confirms it is a valid track by parsing its metadata, then uses it to create a filename (see [Track#makeFilename]).
	///
	/// Always stores a copy of the track as an InputStream does not hold a reference to an original file.
	///
	/// @see #EVENT_TRACK_ADDED
	/// @throws IllegalArgumentException if `is` is null
	/// @return true if the track was added, false if it was not a valid FLAC file.
	public static boolean addTrack(@Nonnull InputStream is) throws IOException {
		if(is == null) {
			throw new IllegalArgumentException("Can't add null track");
		}
		BufferedInputStream bis = new BufferedInputStream(is);
		bis.mark(Integer.MAX_VALUE);

		FLACDecoder flacDecoder = new FLACDecoder(bis);
		List<Pair<String, String>> metadata = FLACUtil.extractMetadata(flacDecoder.readMetadata());
		if(metadata == null) {
			return false;
		}

		Path target = library.toPath().resolve(Track.makeFilename(metadata));
		if(target.toFile().exists()) {
			throw new FileAlreadyExistsException(target + " already exists");
		}

		bis.reset();
		bis.mark(0);

		Files.copy(bis, target);

		registerNewTrack(target.toFile());
		return true;
	}

	/// Start properly tracking a newly added track of which the file is already in the library.
	///
	/// @see #EVENT_TRACK_ADDED
	public static void registerNewTrack(File file) throws IOException {
		Track track = new Track(file);
		ClientStorage.MAIN.tracks.put(track.getFile().getName(), track);
		EVENT_TRACK_ADDED.call(track);
		reloadSelection();
	}

	/// Remove an existing track from the library.
	///
	/// @see #EVENT_TRACK_REMOVED
	public static void removeTrack(Track track) {
		if(!ClientStorage.MAIN.tracks.containsValue(track)) {
			throw new IllegalArgumentException("Track is not in library");
		}

		LOGGER.info("Removing track {}", track.getFile());

		ClientStorage.MAIN.tracks.remove(track.getFile().getName());

		if(!track.getFile().delete()) {
			LOGGER.error("Failed to delete file {}", track.getFile());
		}

		EVENT_TRACK_REMOVED.call(track);
		reloadSelection();
	}

	// Export current positive filter options to a network and storage friendly list.
	//
	// @see #exportNegativeOptions
	// @see #importFilterOptions
	public static List<Pair<Integer, String>> exportPositiveOptions() {
		LinkedList<Pair<Integer, String>> toReturn = new LinkedList<>();
		for(Filter filter : ClientStorage.MAIN.filters.get()) {
			for(FilterOption option : filter.getOptions()) {
				if(option.getState() == FilterOption.State.POSITIVE) {
					toReturn.add(new Pair<>(filter.id, option.value));
				}
			}
		}
		return toReturn;
	}

	// Export current negative filter options to a network and storage friendly list.
	//
	// @see #exportPositiveOptions
	// @see #importFilterOptions
	public static List<Pair<Integer, String>> exportNegativeOptions() {
		LinkedList<Pair<Integer, String>> toReturn = new LinkedList<>();
		for(Filter filter : ClientStorage.MAIN.filters.get()) {
			for(FilterOption option : filter.getOptions()) {
				if(option.getState() == FilterOption.State.NEGATIVE) {
					toReturn.add(new Pair<>(filter.id, option.value));
				}
			}
		}
		return toReturn;
	}

	// Imports filter options from a network and storage friendly list.
	//
	// @see #exportNegativeOptions
	// @see #exportPositiveOptions
	public static void importFilterOptions(List<Pair<Integer, String>> positive,
			List<Pair<Integer, String>> negative) {
		ScopedValue.where(Player.DONT_SEND_UPDATES, true).run(() -> {
			collectReloads(() -> {
				for(Filter filter : ClientStorage.MAIN.filters.get()) {
					int id = filter.id;

					runtimeOptions:
					for(FilterOption option : filter.getOptions()) {
						String value = option.value;

						if(positive != null) {
							for(Pair<Integer, String> positiveOption : positive) {
								if(positiveOption.key != id || !Objects.equals(value, positiveOption.value)) {
									continue;
								}

								option.setState(FilterOption.State.POSITIVE);
								continue runtimeOptions;
							}
						}


						if(negative != null) {
							for(Pair<Integer, String> negativeOption : negative) {
								if(negativeOption.key != id || !Objects.equals(value, negativeOption.value)) {
									continue;
								}

								option.setState(FilterOption.State.NEGATIVE);
								continue runtimeOptions;
							}
						}

						option.setState(FilterOption.State.NONE);
					}
				}
			});
		});
		if(Player.shouldSendUpdate()) {
			PlaybackUpdateMessage update = new PlaybackUpdateMessage();
			update.positiveOptions = exportPositiveOptions();
			update.negativeOptions = exportNegativeOptions();
			Server.send(update);
		}
	}

	/// Runs the specified runnable preventing the selection reloads from happening, then does a selection reload. Used
	/// to improve performance when performing actions that would normally trigger one on bulk.
	///
	/// When using this, it is good practice to call the {@link #reloadSelection} method when the intention is for it to
	/// be called regardless of what may have happened above in the runnable. This improves readability and allows potential
	/// future optimizations.
	public static void collectReloads(Runnable runnable) {
		ScopedValue.where(NO_RELOAD_SELECTION, true).run(runnable);
		reloadSelection();
	}

	/// Handles an [Action.Type#ADD] action received from the server.
	///
	/// Equivalent to [#handleReplaceAction] with an extra check to prevent overriding existing files.
	///
	/// @see #EVENT_TRACK_ADDED
	public static void handleAddAction(Action action) throws IOException {
		File target = library.toPath().resolve(action.filename).toFile();
		if(target.exists()) {
			LOGGER.error("Received ADD action for track {} which already exists, ignoring", action.filename);
			return;
		}
		handleReplaceAction(action);
	}

	/// Handles a [Action.Type#REPLACE] action received from the server.
	///
	/// Downloads the track from the server's HTTP endpoint, stores it and adds it to the library.
	///
	/// Uses `.tmp` files to prevent leftover incomplete downloads.
	///
	/// @see ClientStorage.Main#serverFilePort
	public static void handleReplaceAction(Action action) throws IOException {
		File target = library.toPath().resolve(action.filename).toFile();

		HttpsURLConnection conn = Server.startTransferRequest("GET", action.filename);
		int res = conn.getResponseCode();
		if(res != 200) {
			LOGGER.error("Got unexpected response {} while downloading track {}", res, action.filename);
			throw new IllegalStateException(res + " response from server");
		}

		Path tmpTarget = new File(target.getAbsolutePath() + ".tmp").toPath();
		Files.copy(conn.getInputStream(), tmpTarget, StandardCopyOption.REPLACE_EXISTING);
		conn.getInputStream().close();

		try {
			// According to the javadocs, it is implementation specific whether the atomic move is allowed to override
			// an existing file. This forces it to by deleting the file beforehand if it existed.
			var _ = target.delete();
			Files.move(tmpTarget, target.toPath(), StandardCopyOption.ATOMIC_MOVE);
		} catch(IOException _) {
			LOGGER.debug("Atomic move of {} to {} failed, using normal move", tmpTarget, target);
			Files.move(tmpTarget, target.toPath());
		}
		ClientStorage.MAIN.tracks.remove(target.getName());
		registerNewTrack(target);
	}

	/// Handles a [Action.Type#REMOVE] action received from the server.
	///
	/// Removes the file and removes the track from the library.
	///
	/// @throws IOException if the file cannot be removed
	/// @see #EVENT_TRACK_REMOVED
	public static void handleRemoveAction(Action action) throws IOException {
		File target = library.toPath().resolve(action.filename).toFile();
		if(!target.delete()) {
			LOGGER.error("Failed to delete {}. Were permissions messed with?", target);
			throw new IOException("Failed to delete file");
		}

		ClientStorage.MAIN.tracks.remove(target.getName());
		reloadSelection();
	}

	/// Block until library is initialized.
	///
	/// @see #EVENT_LOADED
	public static void waitUntilLoaded() throws InterruptedException {
		synchronized(INITIALIZED) {
			while(!INITIALIZED.get()) {
				INITIALIZED.wait();
			}
		}
	}

	/// Data for [Library#EVENT_SELECTED_TRACKS_UPDATED]
	///
	/// @param oldSelection the old selected tracks, usable if needed to only update needed UI elements.
	/// @param newSelection the new selected tracks, which the UI should match both in contents and in order.
	public record SelectedTracksUpdatedEvent(List<Track> oldSelection, List<Track> newSelection) {}

	/// Data for [Library#EVENT_SORTING_HEADER_UPDATED]
	public record SortingHeaderUpdatedEvent(Header header, Order order) {}

	/// Data for [Library#EVENT_HEADER_MOVED]
	public record HeaderMovedEvent(Header header, int oldPosition, int newPosition) {}

	/// Data for [Library#EVENT_FILTER_MOVED]
	public record FilterMovedEvent(Filter filter, int newPosition) {}
}
