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
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.event.RetroactiveEventSource;
import dev.blackilykat.pmp.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Library {
	public static final RetroactiveEventSource<List<Track>> EVENT_LOADED = new RetroactiveEventSource<>();
	public static final EventSource<Filter> EVENT_FILTER_ADDED = new EventSource<>();
	public static final EventSource<Filter> EVENT_FILTER_REMOVED = new EventSource<>();
	public static final EventSource<SelectedTracksUpdatedEvent> EVENT_SELECTED_TRACKS_UPDATED = new EventSource<>();

	private static final ScopedValue<Boolean> NO_RELOAD_SELECTION = ScopedValue.newInstance();

	private static final Logger LOGGER = LogManager.getLogger(Library.class);

	private static File library = null;
	private static boolean initialized = false;
	private static List<Track> tracks = new LinkedList<>();
	private static List<Track> selectedTracks = new LinkedList<>();
	private static List<Filter> filters = new LinkedList<>();

	public static void reloadSelection() {
		if(NO_RELOAD_SELECTION.orElse(false)) {
			return;
		}
		LOGGER.debug("Reloading selection");

		Set<Track> selection = new HashSet<>(tracks);
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
		selectedTracks = new LinkedList<>(selection.stream().toList());
		EVENT_SELECTED_TRACKS_UPDATED.call(
				new SelectedTracksUpdatedEvent(Collections.unmodifiableList(oldSelectedTracks),
						Collections.unmodifiableList(selectedTracks)));
	}

	public static void addFilter(Filter filter) {
		filters.add(filter);

		collectReloads(() -> {
			FilterOption opt = new FilterOption(Filter.OPTION_EVERYTHING);
			opt.setState(FilterOption.State.POSITIVE);
			filter.addOption(opt);

			reloadSelection();
		});

		EVENT_FILTER_ADDED.call(filter);
	}

	public static void removeFilter(Filter filter) {
		filters.remove(filter);

		reloadSelection();

		EVENT_FILTER_REMOVED.call(filter);
	}

	public static List<Filter> getFilters() {
		return Collections.unmodifiableList(filters);
	}

	public static List<Track> getAllTracks() {
		maybeInit();
		return Collections.unmodifiableList(tracks);
	}

	public static List<Track> getSelectedTracks() {
		maybeInit();
		return Collections.unmodifiableList(selectedTracks);
	}

	public static void maybeInit() {
		if(initialized) {
			return;
		}
		LOGGER.info("Initializing library");

		library = new File("library");
		if(!library.exists()) {
			library.mkdirs();
		}
		if(!library.isDirectory()) {
			LOGGER.error("Library is a file");
			throw new IllegalStateException("Library is a file");
		}

		List<Track> cache = ClientStorage.getInstance().trackCache;
		LOGGER.debug("Cache: {}", cache);

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
						tracks.add(cached);
					}
				}
				if(!wasCached) {
					LOGGER.warn("Track {} was not cached", file.getName());
					try {
						tracks.add(new Track(file));
					} catch(IOException e) {
						LOGGER.error("Error on track {}", file.getName());
					}
				}
			}
		}
		LOGGER.info("{} tracks cached", totalCached);

		ClientStorage.EVENT_SAVING.register(storage -> {
			storage.trackCache = tracks;
		});

		Filter.EVENT_OPTION_CHANGED_STATE.register(evt -> {
			collectReloads(() -> {
				Filter filter = evt.filter();
				FilterOption option = evt.option();
				FilterOption.State oldState = evt.oldState();
				FilterOption.State state = evt.newState();

				if(filter != null) {
					if(option.value.equals(Filter.OPTION_EVERYTHING)) {
						if(state == FilterOption.State.POSITIVE) {
							for(FilterOption otherOption : filter.getOptions()) {
								if(otherOption != option && otherOption.getState() == FilterOption.State.POSITIVE) {
									otherOption.setState(FilterOption.State.NONE);
								}
							}
						} else if(state == FilterOption.State.NONE) {
							boolean otherPositive = false;

							for(FilterOption otherOption : filter.getOptions()) {
								if(otherOption != option && otherOption.getState() == FilterOption.State.POSITIVE) {
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

				reloadSelection();
			});
		});

		addFilter(new Filter("artist"));
		addFilter(new Filter("album"));

		EVENT_LOADED.call(tracks);

		initialized = true;
		LOGGER.info("Initialized library");
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
}
