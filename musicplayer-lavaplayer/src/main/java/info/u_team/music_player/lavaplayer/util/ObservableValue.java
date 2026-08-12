package info.u_team.music_player.lavaplayer.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObservableValue<T> {
	
	private volatile T value;
	private final List<ChangeListener<T>> changeListener;
	
	public ObservableValue(T value) {
		this.value = value;
		changeListener = new CopyOnWriteArrayList<>();
	}
	
	public void registerListener(ChangeListener<T> listener) {
		changeListener.add(listener);
	}
	
	public void setValue(T value) {
		this.value = value;
		changeListener.forEach(listener -> listener.update(value));
	}
	
	public T getValue() {
		return value;
	}
	
	@FunctionalInterface
	public interface ChangeListener<T> {
		
		void update(T value);
	}
	
}
