package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.Locale;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.integration.SharedListeningService;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiSharedListening extends BetterScreen {

	private final Screen previous;
	private EditBox host;
	private EditBox port;
	private EditBox code;
	private UButton status;
	private String transientStatus = "";
	private long transientUntil;

	public GuiSharedListening(Screen previous) {
		super(Component.literal(getTranslation("gui.shared.title")));
		this.previous = previous;
	}

	@Override
	protected void init() {
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		addRenderableWidget(new UButton(8, 8, 28, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previous)));
		status = disabled(40, 8, width - 48, getTranslation("gui.shared.inactive"));
		host = new EditBox(font, 12, 40, width - 24, 20, Component.literal(getTranslation("gui.shared.host")));
		host.setValue("127.0.0.1"); host.setMaxLength(255); addRenderableWidget(host);
		port = new EditBox(font, 12, 64, (width - 28) / 2, 20, Component.literal(getTranslation("gui.shared.port")));
		port.setValue("27841"); port.setMaxLength(5); addRenderableWidget(port);
		code = new EditBox(font, width / 2 + 2, 64, (width - 28) / 2, 20, Component.literal(getTranslation("gui.shared.code")));
		code.setMaxLength(8); addRenderableWidget(code);

		final int third = (width - 36) / 3;
		addRenderableWidget(new UButton(12, 88, third, 20, Component.literal(getTranslation("gui.shared.direct_host")), button -> directHost()));
		addRenderableWidget(new UButton(16 + third, 88, third, 20, Component.literal(getTranslation("gui.shared.direct_join")), button -> directJoin()));
		addRenderableWidget(new UButton(20 + third * 2, 88, third, 20, Component.literal(getTranslation("gui.shared.disconnect")), button -> { service().disconnect(); refresh(); }));
		final int half = (width - 28) / 2;
		addRenderableWidget(new UButton(12, 112, half, 20, Component.literal(getTranslation("gui.shared.relay_broadcast")), button -> relayBroadcast()));
		addRenderableWidget(new UButton(width / 2 + 2, 112, half, 20, Component.literal(getTranslation("gui.shared.relay_join")), button -> relayJoin()));
		toggle(12, 140, half, "gui.shared.broadcast", settings::isNearbyMusicBroadcast, settings::setNearbyMusicBroadcast);
		toggle(width / 2 + 2, 140, half, "gui.shared.receive", settings::isNearbyMusicReceive, settings::setNearbyMusicReceive);
		disabled(12, 168, width - 24, getTranslation("gui.shared.notice"));
		refresh();
	}

	@Override
	public void tick() { refresh(); }

	private void directHost() {
		try {
			code.setValue(service().host(parsePort()));
			MusicPlayerManager.getSettingsManager().getSettings().setNearbyMusicBroadcast(true);
			refresh();
		} catch (final Exception exception) { showError("Direct host failed: " + exception.getMessage()); }
	}

	private void directJoin() {
		try {
			requireReceive();
			service().join(host.getValue(), parsePort(), codeValue());
			refresh();
		} catch (final Exception exception) { showError("Direct join failed: " + exception.getMessage()); }
	}

	private void relayBroadcast() {
		try {
			final String generated = service().broadcastThroughRelay(host.getValue(), parsePort(), codeValue());
			code.setValue(generated);
			MusicPlayerManager.getSettingsManager().getSettings().setNearbyMusicBroadcast(true);
			refresh();
		} catch (final Exception exception) { showError("Relay broadcast failed: " + exception.getMessage()); }
	}

	private void relayJoin() {
		try {
			requireReceive();
			service().joinRelay(host.getValue(), parsePort(), codeValue());
			refresh();
		} catch (final Exception exception) { showError("Relay join failed: " + exception.getMessage()); }
	}

	private void requireReceive() {
		if (!MusicPlayerManager.getSettingsManager().getSettings().isNearbyMusicReceive()) throw new IllegalStateException("Enable Receive state first");
	}

	private String codeValue() { return code.getValue().strip().toUpperCase(Locale.ROOT); }
	private int parsePort() { try { return Integer.parseInt(port.getValue()); } catch (final NumberFormatException exception) { return 27_841; } }

	private void refresh() {
		if (status == null) return;
		if (System.currentTimeMillis() < transientUntil) { status.setMessage(Component.literal(transientStatus)); return; }
		status.setMessage(Component.literal(service().status() + (service().sessionCode().isBlank() ? "" : " — " + getTranslation("gui.shared.session_code", service().sessionCode()))));
	}

	private void showError(String value) { transientStatus = value; transientUntil = System.currentTimeMillis() + 5000L; refresh(); }
	private UButton disabled(int x, int y, int buttonWidth, String value) { final UButton button = addRenderableWidget(new UButton(x, y, buttonWidth, 20, Component.literal(value))); button.active = false; return button; }
	private void toggle(int x, int y, int buttonWidth, String key, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
		final UButton button = addRenderableWidget(new UButton(x, y, buttonWidth, 20, toggleLabel(key, getter.getAsBoolean())));
		button.setPressable(() -> { setter.accept(!getter.getAsBoolean()); button.setMessage(toggleLabel(key, getter.getAsBoolean())); });
	}
	private static Component toggleLabel(String key, boolean enabled) { return Component.literal(getTranslation(key) + ": " + getTranslation(enabled ? "gui.settings.state.on" : "gui.settings.state.off")); }
	private SharedListeningService service() { return MusicPlayerManager.getSharedListeningService(); }
}
