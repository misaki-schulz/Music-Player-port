package info.u_team.music_player.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import info.u_team.music_player.lavaplayer.api.search.ISearchResult;
import info.u_team.music_player.lavaplayer.api.search.ITrackSearch;

/** Call load/cancel on the delivery thread; callbacks are marshalled to that thread. */
public final class OrderedTrackLoader {

	private final ITrackSearch search;
	private final Executor delivery;
	private int generation;
	private boolean loading;

	public OrderedTrackLoader(ITrackSearch search, Executor delivery) {
		this.search = search;
		this.delivery = delivery;
	}

	public void cancel() {
		generation++;
		loading = false;
	}

	public boolean isLoading() {
		return loading;
	}

	public void load(List<String> uris, Consumer<ISearchResult> consumer, Runnable finished) {
		cancel();
		final int request = generation;
		final List<ISearchResult> results = new ArrayList<>(Collections.nCopies(uris.size(), null));
		final boolean[] ready = new boolean[uris.size()];
		final int[] next = { 0 };
		loading = !uris.isEmpty();
		if (!loading) {
			finished.run();
			return;
		}
		for (int index = 0; index < uris.size(); index++) {
			final int slot = index;
			search.getTracks(uris.get(index), result -> delivery.execute(() -> {
				if (request != generation || ready[slot]) return;
				results.set(slot, result);
				ready[slot] = true;
				while (next[0] < results.size() && ready[next[0]]) {
					consumer.accept(results.get(next[0]++));
					if (request != generation) return;
				}
				if (next[0] == results.size()) {
					loading = false;
					finished.run();
				}
			}));
		}
	}
}
