package info.u_team.music_player.musicplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.util.SafeFileStorage;

public final class EqualizerPresetManager implements IGsonLoadable {

	private static final Map<String, float[]> BUILT_INS = createBuiltIns();
	private final Gson gson;
	private Path path;
	private PresetFile file = new PresetFile();
	private String selected = "Flat";

	EqualizerPresetManager(Gson gson) { this.gson = gson; }

	@Override public void setBasePath(Path basePath) { path = basePath.resolve("equalizer-presets.json"); }
	@Override public synchronized void loadFromFile() {
		file = new PresetFile();
		for (final Path candidate : SafeFileStorage.readCandidates(path)) {
			if (!Files.isRegularFile(candidate)) continue;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(candidate), StandardCharsets.UTF_8))) {
				final PresetFile loaded = gson.fromJson(reader, PresetFile.class);
				if (loaded != null && loaded.presets != null) { file = loaded; break; }
			} catch (final IOException | RuntimeException ignored) { }
		}
		normalize();
		writeToFile();
	}
	@Override public synchronized void writeToFile() {
		if (path == null) return;
		try {
			SafeFileStorage.writeAtomically(path, output -> {
				final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
				gson.toJson(file, writer); writer.flush();
			});
		} catch (final IOException ignored) { }
	}

	public synchronized String getSelected() { return selected; }
	public synchronized String selectNext(Settings settings) {
		final List<String> names = names();
		final int index = Math.max(0, names.indexOf(selected));
		selected = names.get((index + 1) % names.size());
		settings.setEqualizerGains(gains(selected));
		return selected;
	}
	public synchronized String saveCurrent(Settings settings) {
		int index = 1;
		while (file.presets.containsKey("Custom " + index)) index++;
		selected = "Custom " + index;
		file.presets.put(selected, settings.getEqualizerGains());
		writeToFile();
		return selected;
	}
	public synchronized boolean deleteSelected() {
		if (BUILT_INS.containsKey(selected) || file.presets.remove(selected) == null) return false;
		selected = "Flat";
		writeToFile();
		return true;
	}
	public synchronized void exportFile(Path destination) throws IOException {
		SafeFileStorage.writeAtomically(destination, output -> {
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
			gson.toJson(file, writer); writer.flush();
		});
	}
	public synchronized int importFile(Path source) throws IOException {
		if (!Files.isRegularFile(source) || Files.size(source) > 1024 * 1024) throw new IOException("Preset file is missing or too large");
		final PresetFile imported;
		try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) { imported = gson.fromJson(reader, PresetFile.class); }
		catch (final RuntimeException exception) { throw new IOException("Invalid preset JSON", exception); }
		if (imported == null || imported.presets == null) throw new IOException("Preset JSON is empty");
		int count = 0;
		for (final var entry : imported.presets.entrySet()) {
			if (entry.getKey() == null || entry.getKey().isBlank() || !valid(entry.getValue())) continue;
			file.presets.put(entry.getKey().strip().substring(0, Math.min(80, entry.getKey().strip().length())), Arrays.copyOf(entry.getValue(), Settings.EQ_BAND_COUNT));
			count++;
		}
		normalize(); writeToFile(); return count;
	}

	private List<String> names() { final List<String> names = new ArrayList<>(BUILT_INS.keySet()); names.addAll(file.presets.keySet()); return names; }
	private float[] gains(String name) { final float[] gains = BUILT_INS.containsKey(name) ? BUILT_INS.get(name) : file.presets.get(name); return Arrays.copyOf(gains, gains.length); }
	private void normalize() { file.presets.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isBlank() || !valid(entry.getValue()) || BUILT_INS.containsKey(entry.getKey())); }
	private static boolean valid(float[] gains) { if (gains == null || gains.length != Settings.EQ_BAND_COUNT) return false; for (final float gain : gains) if (!Float.isFinite(gain)) return false; return true; }
	private static Map<String, float[]> createBuiltIns() {
		final Map<String, float[]> presets = new LinkedHashMap<>();
		presets.put("Flat", new float[10]);
		presets.put("Bass", new float[] { 8, 7, 5, 3, 1, 0, -1, -2, -2, -2 });
		presets.put("Treble", new float[] { -3, -2, -1, 0, 1, 3, 5, 7, 8, 8 });
		presets.put("V shape", new float[] { 7, 5, 3, 0, -2, -2, 0, 3, 5, 7 });
		presets.put("Vocal", new float[] { -3, -2, 0, 3, 5, 5, 3, 1, -1, -2 });
		return presets;
	}
	private static final class PresetFile { private int formatVersion = 1; private Map<String, float[]> presets = new LinkedHashMap<>(); }
}
