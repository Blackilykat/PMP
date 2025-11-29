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

import dev.blackilykat.pmp.Filter;
import dev.blackilykat.pmp.FilterOption;
import dev.blackilykat.pmp.Order;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.event.RetroactiveEventSource;
import dev.blackilykat.pmp.messages.PlaybackControlMessage;
import dev.blackilykat.pmp.messages.PlaybackUpdateMessage;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Library {
	public static final RetroactiveEventSource<Collection<Track>> EVENT_LOADED = new RetroactiveEventSource<>();
	public static final EventSource<Filter> EVENT_FILTER_ADDED = new EventSource<>();
	public static final EventSource<Filter> EVENT_FILTER_REMOVED = new EventSource<>();
	public static final EventSource<Track> EVENT_TRACK_ADDED = new EventSource<>();
	public static final EventSource<Track> EVENT_TRACK_REMOVED = new EventSource<>();
	public static final EventSource<Header> EVENT_HEADER_ADDED = new EventSource<>();
	public static final EventSource<Header> EVENT_HEADER_REMOVED = new EventSource<>();
	public static final EventSource<HeaderMovedEvent> EVENT_HEADER_MOVED = new EventSource<>();
	public static final EventSource<FilterMovedEvent> EVENT_FILTER_MOVED = new EventSource<>();
	public static final EventSource<SelectedTracksUpdatedEvent> EVENT_SELECTED_TRACKS_UPDATED = new EventSource<>();
	public static final EventSource<SortingHeaderUpdatedEvent> EVENT_SORTING_HEADER_UPDATED = new EventSource<>();

	private static final ScopedValue<Boolean> NO_RELOAD_SELECTION = ScopedValue.newInstance();
	private static final ScopedValue<Boolean> IGNORE_FILTER_OPTION_UPDATE = ScopedValue.newInstance();

	private static final Logger LOGGER = LogManager.getLogger(Library.class);
	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

	private static File library = null;
	private static Map<String, Track> tracks = new HashMap<>();
	private static List<Track> selectedTracks = new LinkedList<>();
	private static List<Filter> filters = new LinkedList<>();
	private static List<Header> headers = new LinkedList<>();
	private static Header sortingHeader = null;
	private static Order sortingOrder = Order.ASCENDING;

	public static void reloadSelection() {
		if(NO_RELOAD_SELECTION.orElse(false)) {
			return;
		}
		LOGGER.debug("Reloading selection");

		Set<Track> selection = new HashSet<>(tracks.values());
		Set<Track> newSelection = new HashSet<>();

		for(Filter filter : filters) {
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

			filter.applyOptionValues(foundValues.stream().toList());

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
		selectedTracks = new LinkedList<>(selection.stream().sorted((a, b) -> {
			if(sortingHeader == null || sortingOrder == null) {
				return 0;
			}
			int multiplier = sortingOrder == Order.ASCENDING ? 1 : -1;
			return sortingHeader.compare(a, b) * multiplier;
		}).toList());
		EVENT_SELECTED_TRACKS_UPDATED.call(
				new SelectedTracksUpdatedEvent(Collections.unmodifiableList(oldSelectedTracks),
						Collections.unmodifiableList(selectedTracks)));
	}

	public static void addFilter(Filter filter) {
		LOGGER.info("Adding filter {}", filter);
		filters.add(filter);

		collectReloads(() -> {
			FilterOption opt = new FilterOption(Filter.OPTION_EVERYTHING);
			filter.addOption(opt);
			opt.setState(FilterOption.State.POSITIVE);

			reloadSelection();
		});

		EVENT_FILTER_ADDED.call(filter);
	}

	public static void removeFilter(Filter filter) {
		LOGGER.info("Removing filter {}", filter);
		filters.remove(filter);

		reloadSelection();

		EVENT_FILTER_REMOVED.call(filter);
	}

	public static void moveFilter(Filter filter, int position) {
		if(position < 0 || position >= filters.size()) {
			throw new IndexOutOfBoundsException(
					"Position " + position + " is out of bounds for size " + filters.size());
		}

		int oldPos = filters.indexOf(filter);
		if(oldPos == -1) {
			throw new IllegalArgumentException("Filter " + filter + " is not in filters");
		}

		LOGGER.info("Moving filter {} from {} to {}", filter, oldPos, position);

		filters.remove(filter);
		filters.add(position, filter);

		reloadSelection();

		EVENT_FILTER_MOVED.call(new FilterMovedEvent(filter, oldPos, position));
	}

	public static List<Header> getHeaders() {
		return Collections.unmodifiableList(headers);
	}

	public static void addHeader(Header header) {
		LOGGER.info("Adding header {}", header);


		headers.add(header);

		ClientStorage.getInstance().setHeaders(headers);

		EVENT_HEADER_ADDED.call(header);
	}

	public static void moveHeader(Header header, int position) {
		if(!headers.contains(header)) {
			throw new IllegalArgumentException(header + " is not in headers");
		}
		if(position < 0 || position >= headers.size()) {
			throw new IndexOutOfBoundsException(
					"Position " + position + " out of bounds for " + headers.size() + " headers");
		}

		LOGGER.debug("Moving {} to position {}", header, position);

		int oldPosition = headers.indexOf(header);

		headers.remove(header);

		headers.add(position, header);

		ClientStorage.getInstance().setHeaders(headers);

		EVENT_HEADER_MOVED.call(new HeaderMovedEvent(header, oldPosition, position));
	}

	/**
	 * @throws IllegalStateException if header is the last header
	 */
	public static void removeHeader(Header header) {
		if(headers.size() == 1) {
			throw new IllegalStateException("Can't remove last header");
		}

		LOGGER.info("Removing header {}", header);

		if(header == sortingHeader) {
			sortingHeader = headers.getFirst();
			sortingOrder = Order.ASCENDING;
		}

		headers.remove(header);

		ClientStorage.getInstance().setHeaders(headers);

		EVENT_HEADER_REMOVED.call(header);
		header.eventHeaderRemoved.call(null);
	}

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

	public static List<Filter> getFilters() {
		return Collections.unmodifiableList(filters);
	}

	public static Collection<Track> getAllTracks() {
		return Collections.unmodifiableCollection(tracks.values());
	}

	public static List<Track> getSelectedTracks() {
		return Collections.unmodifiableList(selectedTracks);
	}

	public static void init() {
		synchronized(INITIALIZED) {
			if(INITIALIZED.get()) {
				throw new IllegalStateException("Already initialized");
			}
			INITIALIZED.set(true);
			LOGGER.info("Initializing library");

			library = new File("library");
			if(!library.exists()) {
				var _ = library.mkdirs();
			}
			if(!library.isDirectory()) {
				LOGGER.error("Library is a file");
				throw new IllegalStateException("Library is a file");
			}

			List<Track> cache = ClientStorage.getInstance().getTrackCache();

			File[] children = library.listFiles();
			int totalCached = 0;
			if(children != null) {
				for(File file : children) {
					boolean wasCached = false;
					for(Track cached : cache) {
						if(cached.getFile().getName().equals(file.getName())) {
							if(file.lastModified() != cached.getLastModified()) {
								break;
							}
							totalCached++;
							wasCached = true;
							tracks.put(file.getName(), cached);
						}
					}
					if(!wasCached) {
						LOGGER.warn("Track {} was not cached", file.getName());
						try {
							Track track = new Track(file);
							tracks.put(file.getName(), track);
						} catch(IOException e) {
							LOGGER.error("Error on track {}", file.getName());
						}
					}
				}
			}
			LOGGER.info("{} tracks cached", totalCached);

			ClientStorage.EVENT_MAYBE_SAVING.register(event -> {
				ClientStorage storage = event.clientStorage;

				// imperfect check but the computational effort is probably not worth it considering there is no data
				// loss involved (and it usually saves on shutdown anyway)
				if(storage.getTrackCache().size() != tracks.size()) {
					event.markDirty();
				}
			});

			ClientStorage.EVENT_SAVING.register(storage -> {
				storage.setTrackCache(tracks.values().stream().toList());
			});

			ClientStorage storage = ClientStorage.getInstance();

			List<Header> storedHeaders = storage.getHeaders();
			if(storedHeaders == null) {
				headers.add(new Header(0, "N°", "tracknumber"));
				headers.add(new Header(1, "Title", "title"));
				headers.add(new Header(2, "Artist", "artist"));
				headers.add(new Header(3, "Duration", "duration"));
				storage.setHeaders(headers);
				storage.setCurrentHeaderID(4);
			} else {
				for(Header storedHeader : storedHeaders) {
					storedHeader.updateType();
					headers.add(storedHeader);
				}
			}
			LOGGER.debug("Headers: {}", headers);

			sortingHeader = headers.getFirst();
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
				List<Filter> storedFilters = storage.getFilters();
				if(storedFilters == null) {
					addFilter(new Filter("artist"));
					addFilter(new Filter("album"));

					storage.setFilters(filters);
				} else {
					for(Filter filter : storedFilters) {
						addFilter(filter);
					}
				}
			}

			ClientStorage.EVENT_MAYBE_SAVING.register(event -> {
				ClientStorage cs = event.clientStorage;

				List<Filter> storedFilters = cs.getFilters();
				if(storedFilters.size() != filters.size()) {
					event.markDirty();
					return;
				}

				for(int i = 0; i < storedFilters.size(); i++) {
					Filter a = storedFilters.get(i);
					Filter b = filters.get(i);
					if(a.id != b.id || !a.key.equals(b.key)) {
						event.markDirty();
						return;
					}
				}
			});

			ClientStorage.EVENT_SAVING.register(cs -> {
				cs.setFilters(filters);
			});

			EVENT_LOADED.call(tracks.values());

			LOGGER.info("Initialized library");
		}
	}

	public static boolean addTrack(File file) throws IOException {
		if(file == null || !file.exists()) {
			throw new IllegalArgumentException("File does not exist");
		}
		if(file.isDirectory()) {
			throw new IllegalArgumentException("File is a directory");
		}
		List<Pair<String, String>> metadata = Track.extractMetadata(file);
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

		Track track = new Track(target.toFile());
		tracks.put(track.getFile().getName(), track);
		EVENT_TRACK_ADDED.call(track);
		reloadSelection();
		return true;
	}

	public static void removeTrack(Track track) {
		if(!tracks.containsValue(track)) {
			throw new IllegalArgumentException("Track is not in library");
		}

		//noinspection LoggingSimilarMessage
		LOGGER.info("Removing track {}", track.getFile());

		tracks.remove(track.getFile().getName());

		if(!track.getFile().delete()) {
			LOGGER.error("Failed to delete file {}", track.getFile());
		}

		EVENT_TRACK_REMOVED.call(track);
		reloadSelection();
	}

	public static List<Pair<Integer, String>> exportPositiveOptions() {
		LinkedList<Pair<Integer, String>> toReturn = new LinkedList<>();
		for(Filter filter : getFilters()) {
			for(FilterOption option : filter.getOptions()) {
				if(option.getState() == FilterOption.State.POSITIVE) {
					toReturn.add(new Pair<>(filter.id, option.value));
				}
			}
		}
		return toReturn;
	}

	public static void importFilterOptions(List<Pair<Integer, String>> positive,
			List<Pair<Integer, String>> negative) {
		ScopedValue.where(Player.DONT_SEND_UPDATES, true).run(() -> {
			collectReloads(() -> {
				for(Filter filter : getFilters()) {
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


						boolean isNegative = false;
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

	public static List<Pair<Integer, String>> exportNegativeOptions() {
		LinkedList<Pair<Integer, String>> toReturn = new LinkedList<>();
		for(Filter filter : getFilters()) {
			for(FilterOption option : filter.getOptions()) {
				if(option.getState() == FilterOption.State.NEGATIVE) {
					toReturn.add(new Pair<>(filter.id, option.value));
				}
			}
		}
		return toReturn;
	}

	public static Track getTrackByFilename(String filename) {
		return tracks.get(filename);
	}

	/**
	 * Runs the specified runnable preventing the selection reloads from happening, then does a selection reload. Used
	 * to improve performance when performing actions that would normally trigger one on bulk.
	 * <p>
	 * When using this, it is good practice to call the {@link #reloadSelection} method when the intention is for it to
	 * be called regardless of what may have happened above in the runnable.
	 */
	public static void collectReloads(Runnable runnable) {
		ScopedValue.where(NO_RELOAD_SELECTION, true).run(runnable);
		reloadSelection();
	}

	public record SelectedTracksUpdatedEvent(List<Track> oldSelection, List<Track> newSelection) {}

	public record SortingHeaderUpdatedEvent(Header header, Order order) {}

	public record HeaderMovedEvent(Header header, int oldPosition, int newPosition) {}

	public record FilterMovedEvent(Filter filter, int oldPosition, int newPosition) {}
}
