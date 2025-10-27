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

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.blackilykat.pmp.event.EventSource;
import dev.blackilykat.pmp.util.Pair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Session implements Cloneable {
	public final EventSource<String> eventTrackChanged = new EventSource<>();
	public final EventSource<Boolean> eventPlayingChanged = new EventSource<>();
	public final EventSource<Long> eventPlaybackEpochChanged = new EventSource<>();
	public final EventSource<Long> eventPlaybackPositionChanged = new EventSource<>();
	public final EventSource<List<Pair<Integer, String>>> eventPositiveFilterOptionsChanged = new EventSource<>();
	public final EventSource<List<Pair<Integer, String>>> eventNegativeFilterOptionsChanged = new EventSource<>();
	public final EventSource<RepeatOption> eventRepeatChanged = new EventSource<>();
	public final EventSource<ShuffleOption> eventShuffleChanged = new EventSource<>();
	public final EventSource<String> eventNextTrackChanged = new EventSource<>();


	public final int id;
	/**
	 * Filename of the track that is currently selected (may be playing or paused). Can be null.
	 */
	private String track;
	/**
	 * Whether any device is currently playing in this session. Should be false for every session at startup.
	 */
	private boolean playing = false;
	/**
	 * Unix timestamp used to keep track of the current playback position when ${@link #playing} == true.
	 */
	private long playbackEpoch = -1;
	/**
	 * Current playback position when {@link #playing} == false. In milliseconds.
	 */
	private long playbackPosition = 0;
	/**
	 * List of positive filter options. Each element is composed by an Integer denoting the {@link Filter#id} and the
	 * value of the option.
	 */
	private List<Pair<Integer, String>> positiveFilterOptions = new LinkedList<>();

	/**
	 * List of negative filter options. Each element is composed by an Integer denoting the {@link Filter#id} and the
	 * value of the option.
	 */
	private List<Pair<Integer, String>> negativeFilterOptions = new LinkedList<>();

	private RepeatOption repeat = RepeatOption.OFF;
	private ShuffleOption shuffle = ShuffleOption.OFF;

	/**
	 * Next track which will get immediately played at the end of this one. Can be null if playback stops after this
	 * track.
	 */
	private String nextTrack = null;

	public Session() {
		this(Storage.getStorage().getAndIncrementCurrentSessionId());
	}

	@JsonCreator
	public Session(int id) {
		this.id = id;
	}

	public String getTrack() {
		return track;
	}

	public void setTrack(String track) {
		this.track = track;
		eventTrackChanged.call(track);
	}

	public boolean getPlaying() {
		return playing;
	}

	public void setPlaying(boolean playing) {
		this.playing = playing;
		eventPlayingChanged.call(playing);
	}

	public long getPlaybackEpoch() {
		return playbackEpoch;
	}

	public void setPlaybackEpoch(long playbackEpoch) {
		this.playbackEpoch = playbackEpoch;
		eventPlaybackEpochChanged.call(playbackEpoch);
	}

	public long getPlaybackPosition() {
		return playbackPosition;
	}

	public void setPlaybackPosition(long playbackPosition) {
		this.playbackPosition = playbackPosition;
		eventPlaybackPositionChanged.call(playbackPosition);
	}

	public List<Pair<Integer, String>> getPositiveFilterOptions() {
		return Collections.unmodifiableList(positiveFilterOptions);
	}

	public void setPositiveFilterOptions(List<Pair<Integer, String>> positiveFilterOptions) {
		this.positiveFilterOptions = new LinkedList<>(positiveFilterOptions);
		eventPositiveFilterOptionsChanged.call(this.positiveFilterOptions);
	}

	public List<Pair<Integer, String>> getNegativeFilterOptions() {
		return Collections.unmodifiableList(negativeFilterOptions);
	}

	public void setNegativeFilterOptions(List<Pair<Integer, String>> negativeFilterOptions) {
		this.negativeFilterOptions = new LinkedList<>(negativeFilterOptions);
		eventNegativeFilterOptionsChanged.call(this.negativeFilterOptions);
	}

	public RepeatOption getRepeat() {
		return repeat;
	}

	public void setRepeat(RepeatOption repeat) {
		this.repeat = repeat;
		eventRepeatChanged.call(repeat);
	}

	public ShuffleOption getShuffle() {
		return shuffle;
	}

	public void setShuffle(ShuffleOption shuffle) {
		this.shuffle = shuffle;
		eventShuffleChanged.call(shuffle);
	}

	public String getNextTrack() {
		return nextTrack;
	}

	public void setNextTrack(String nextTrack) {
		this.nextTrack = nextTrack;
		eventNextTrackChanged.call(nextTrack);
	}

	@Override
	public Session clone() {
		try {
			Session clone = (Session) super.clone();

			clone.positiveFilterOptions = new LinkedList<>(positiveFilterOptions);
			clone.negativeFilterOptions = new LinkedList<>(negativeFilterOptions);

			return clone;
		} catch(CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	@Override
	public String toString() {
		return String.format("Session#%d(%s, %s, Epoch: %s, Pos: %d, shuffle: %s, repeat: %s)", id, track,
				playing ? "Playing" : "Not playing", playbackEpoch, playbackPosition, shuffle, repeat);
	}
}
