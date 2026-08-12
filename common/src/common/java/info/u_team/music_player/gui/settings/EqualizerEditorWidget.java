package info.u_team.music_player.gui.settings;

import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.EqualizerMode;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

final class EqualizerEditorWidget implements Renderable, GuiEventListener, NarratableEntry {

	private static final String[] LABELS = { "31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k" };
	private final int x, y, width, height;
	private int draggedBand = -1;
	private boolean focused;

	EqualizerEditorWidget(int x, int y, int width, int height) {
		this.x = x; this.y = y; this.width = width; this.height = height;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		final float[] gains = settings.getEqualizerGains();
		final float[] positions = settings.getEqualizerPositions();
		final int plotHeight = Math.max(20, height - 14);
		graphics.fill(x, y, x + width, y + plotHeight, 0xD0181D22);
		graphics.fill(x, y + plotHeight / 2, x + width, y + plotHeight / 2 + 1, 0xFF67717A);
		if (settings.getEqualizerMode() == EqualizerMode.PARAMETRIC) renderCurve(graphics, gains, positions, plotHeight);
		else renderBars(graphics, gains, plotHeight);
		for (int band = 0; band < gains.length; band++) {
			final boolean parametric = settings.getEqualizerMode() == EqualizerMode.PARAMETRIC;
			final int center = parametric ? pointX(positions[band]) : bandX(band);
			final String label = parametric ? frequencyLabel(positions[band]) : LABELS[band];
			final int labelX = Math.clamp(center - Minecraft.getInstance().font.width(label) / 2, x, x + width - Minecraft.getInstance().font.width(label));
			graphics.text(Minecraft.getInstance().font, label, labelX, y + plotHeight + 3, 0xFFCBD5DF, false);
		}
	}

	private void renderBars(GuiGraphicsExtractor graphics, float[] gains, int plotHeight) {
		final int zeroY = gainY(0F, plotHeight);
		for (int band = 0; band < gains.length; band++) {
			final int center = bandX(band);
			final int knobY = gainY(gains[band], plotHeight);
			graphics.fill(center - 1, y + 3, center + 1, y + plotHeight - 3, 0xFF46515C);
			graphics.fill(center - 2, Math.min(zeroY, knobY), center + 2, Math.max(zeroY, knobY) + 1, gains[band] >= 0F ? 0xFF75E0B5 : 0xFFE07878);
			graphics.fill(center - Math.max(3, width / 40), knobY - 3, center + Math.max(3, width / 40), knobY + 4, 0xFF75E0B5);
		}
	}

	private void renderCurve(GuiGraphicsExtractor graphics, float[] gains, float[] positions, int plotHeight) {
		int previousY = gainY(interpolate(gains, positions, 0F), plotHeight);
		for (int pixel = 0; pixel < width; pixel++) {
			final float position = pixel / Math.max(1F, width - 1F);
			final int currentY = gainY(interpolate(gains, positions, position), plotHeight);
			graphics.fill(x + pixel, Math.min(previousY, currentY), x + pixel + 1, Math.max(previousY, currentY) + 2, 0xFF75E0B5);
			previousY = currentY;
		}
		for (int band = 0; band < gains.length; band++) {
			final int center = pointX(positions[band]);
			final int pointY = gainY(gains[band], plotHeight);
			graphics.fill(center - 3, pointY - 3, center + 4, pointY + 4, band == draggedBand ? 0xFFFFCC66 : 0xFFB8F3DB);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		draggedBand = settings.getEqualizerMode() == EqualizerMode.PARAMETRIC ? nearestPoint(event.x(), event.y(), settings) : bandAt(event.x());
		if (doubleClick) settings.setEqualizerPoint(draggedBand, settings.getEqualizerPositions()[draggedBand], 0F);
		else update(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggedBand < 0 || event.button() != 0) return false;
		update(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() != 0 || draggedBand < 0) return false;
		MusicPlayerManager.getSettingsManager().getSettings().commitEqualizerEdit();
		draggedBand = -1;
		return true;
	}

	private void update(double mouseX, double mouseY) {
		final int plotHeight = Math.max(20, height - 14);
		final float normalized = 1F - (float) Math.clamp((mouseY - y) / plotHeight, 0D, 1D);
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		final float gain = Settings.MIN_EQ_GAIN + normalized * (Settings.MAX_EQ_GAIN - Settings.MIN_EQ_GAIN);
		if (settings.getEqualizerMode() == EqualizerMode.PARAMETRIC) settings.previewEqualizerPoint(draggedBand, (float) Math.clamp((mouseX - x) / Math.max(1D, width - 1D), 0D, 1D), gain);
		else settings.previewEqualizerGain(draggedBand, gain);
	}

	private int bandAt(double mouseX) { return Math.clamp((int) ((mouseX - x) * Settings.EQ_BAND_COUNT / width), 0, Settings.EQ_BAND_COUNT - 1); }
	private int bandX(int band) { return x + (2 * band + 1) * width / (Settings.EQ_BAND_COUNT * 2); }
	private int pointX(float position) { return x + Math.round(position * (width - 1)); }
	private int gainY(float gain, int plotHeight) { return y + Math.round((Settings.MAX_EQ_GAIN - gain) / (Settings.MAX_EQ_GAIN - Settings.MIN_EQ_GAIN) * plotHeight); }
	private int nearestPoint(double mouseX, double mouseY, Settings settings) { final float[] gains = settings.getEqualizerGains(), positions = settings.getEqualizerPositions(); final int plotHeight = Math.max(20, height - 14); int nearest = 0; double distance = Double.MAX_VALUE; for (int band = 0; band < gains.length; band++) { final double dx = mouseX - pointX(positions[band]), dy = mouseY - gainY(gains[band], plotHeight), candidate = dx * dx + dy * dy; if (candidate < distance) { distance = candidate; nearest = band; } } return nearest; }
	private static float interpolate(float[] gains, float[] positions, float point) { if (point <= positions[0]) return gains[0]; for (int upper = 1; upper < positions.length; upper++) if (point <= positions[upper]) { final int lower = upper - 1; final float fraction = (point - positions[lower]) / Math.max(0.0001F, positions[upper] - positions[lower]); final float smooth = (1F - (float) Math.cos(fraction * Math.PI)) * 0.5F; return gains[lower] + (gains[upper] - gains[lower]) * smooth; } return gains[gains.length - 1]; }
	private static String frequencyLabel(float position) { final double frequency = 31D * Math.pow(16000D / 31D, position); return frequency >= 1000D ? String.format(java.util.Locale.ROOT, frequency >= 9950D ? "%.0fk" : "%.1fk", frequency / 1000D) : String.format(java.util.Locale.ROOT, "%.0f", frequency); }
	@Override public boolean isMouseOver(double mouseX, double mouseY) { return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height - 14; }
	@Override public void setFocused(boolean value) { focused = value; }
	@Override public boolean isFocused() { return focused; }
	@Override public NarrationPriority narrationPriority() { return focused ? NarrationPriority.FOCUSED : NarrationPriority.NONE; }
	@Override public void updateNarration(NarrationElementOutput output) { }
}
