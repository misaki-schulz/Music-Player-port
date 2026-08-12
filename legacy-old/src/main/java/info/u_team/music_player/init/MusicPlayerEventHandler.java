// Legacy Fabric event bridge for Minecraft 1.21.2-1.21.8; see NOTICE.
package info.u_team.music_player.init;

import java.util.List;

import com.mojang.blaze3d.platform.Window;
import info.u_team.music_player.MusicPlayerMod;
import info.u_team.music_player.gui.GuiMusicPlayer;
import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.gui.util.LegacyGuiTransform;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.MusicPlayerUtils;
import info.u_team.music_player.musicplayer.SettingsManager;
import info.u_team.music_player.musicplayer.settings.IngameOverlayPosition;
import info.u_team.music_player.render.RenderOverlayMusicDisplay;
import info.u_team.music_player.gui.widget.ScrollingText;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

public class MusicPlayerEventHandler {

	private static final SettingsManager settingsManager = MusicPlayerManager.getSettingsManager();
	private static RenderOverlayMusicDisplay overlayRender;
	private static ScrollingText titleRender;
	private static ScrollingText authorRender;

	private static void onClientKeyboard(Minecraft client) {
		if (client.screen != null) {
			return;
		}
		handleKeyboard(client, MusicPlayerKeys.OPEN.consumeClick(), MusicPlayerKeys.PAUSE.consumeClick(),
				MusicPlayerKeys.SKIP_FORWARD.consumeClick(), MusicPlayerKeys.SKIP_BACK.consumeClick());
	}

	private static void onScreenKeyboard(Screen screen, int keyCode, int scanCode, int modifiers) {
		final boolean open = MusicPlayerKeys.OPEN.matches(keyCode, scanCode);
		if (!open && !settingsManager.getSettings().isKeyWorkInGui()) {
			return;
		}
		handleKeyboard(Minecraft.getInstance(), open, MusicPlayerKeys.PAUSE.matches(keyCode, scanCode),
				MusicPlayerKeys.SKIP_FORWARD.matches(keyCode, scanCode), MusicPlayerKeys.SKIP_BACK.matches(keyCode, scanCode));
	}

	private static void handleKeyboard(Minecraft client, boolean open, boolean pause, boolean skipForward, boolean skipBack) {
		if (open) {
			if (!(client.screen instanceof GuiMusicPlayer)) {
				MusicPlayerMod.LOGGER.info("Opening music player screen");
				client.setScreen(new GuiMusicPlayer());
			}
			return;
		}
		if (!pause && !skipForward && !skipBack) {
			return;
		}
		final ITrackManager manager = MusicPlayerManager.getPlayer().getTrackManager();
		if (pause) {
			if (manager.getCurrentTrack() != null) {
				manager.setPaused(!manager.isPaused());
			}
		} else if (skipForward) {
			if (manager.getCurrentTrack() != null) {
				MusicPlayerUtils.skipForward();
			}
		} else if (skipBack && manager.getCurrentTrack() != null) {
			MusicPlayerUtils.skipBack();
		}
	}

	public static void onKeyInput() {
		onClientKeyboard(Minecraft.getInstance());
	}

	public static void onRenderGameOverlay(GuiGraphics graphics, DeltaTracker partialTick) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen != null || !settingsManager.getSettings().isShowIngameOverlay()) {
			return;
		}
		if (overlayRender == null) {
			overlayRender = new RenderOverlayMusicDisplay();
		}

		final Window window = minecraft.getWindow();
		final int screenWidth = window.getGuiScaledWidth();
		final int screenHeight = window.getGuiScaledHeight();
		final int baseWidth = overlayRender.getWidth();
		final int baseHeight = overlayRender.getHeight();
		final float requestedScale = settingsManager.getSettings().getOverlayScale();
		final float fitScale = Math.min((screenWidth - 6F) / baseWidth, (screenHeight - 6F) / baseHeight);
		final float scale = Math.max(0.1F, Math.min(requestedScale, fitScale));
		final int width = Math.round(baseWidth * scale);
		final int height = Math.round(baseHeight * scale);
		final IngameOverlayPosition position = settingsManager.getSettings().getIngameOverlayPosition();
		final int x = position.isLeft() ? 3 : screenWidth - 3 - width;
		final int y = position.isUp() ? 3 : screenHeight - 3 - height;

		LegacyGuiTransform.transformed(graphics, x, y, scale,
				() -> overlayRender.render(graphics, 0, 0, partialTick.getGameTimeDeltaPartialTick(false)));
	}

	private static void onPreInitScreen(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof PauseScreen && settingsManager.getSettings().isShowIngameMenueOverlay()) {
			screen.children().stream().filter(GuiControls.class::isInstance).map(GuiControls.class::cast).findAny().ifPresent(controls -> {
				titleRender = controls.getTitleRender();
				authorRender = controls.getAuthorRender();
			});
		}
	}

	private static void onPostInitScreen(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof PauseScreen && settingsManager.getSettings().isShowIngameMenueOverlay()) {
			final GuiControls controls = new GuiControls(screen, 3, screen.width);
			if (titleRender != null) {
				controls.copyTitleRendererState(titleRender);
				titleRender = null;
			}
			if (authorRender != null) {
				controls.copyAuthorRendererState(authorRender);
				authorRender = null;
			}
			@SuppressWarnings("unchecked")
			final List<GuiEventListener> children = (List<GuiEventListener>) screen.children();
			children.add(controls);
		}
	}

	private static void onDrawScreenPost(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
			screen.children().stream().filter(GuiControls.class::isInstance).map(GuiControls.class::cast).findAny()
					.ifPresent(controls -> controls.render(graphics, mouseX, mouseY, partialTick));
		}
	}

	private static void onMouseReleasePre(Screen screen, double mouseX, double mouseY, int button) {
		if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
			screen.children().stream().filter(GuiControls.class::isInstance).map(GuiControls.class::cast).findAny()
					.ifPresent(controls -> controls.mouseReleased(mouseX, mouseY, button));
		}
	}

	private static void onScreenTick(Screen screen) {
		if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
			screen.children().stream().filter(GuiControls.class::isInstance).map(GuiControls.class::cast).findAny()
					.ifPresent(GuiControls::tick);
		}
	}

	@SuppressWarnings("deprecation")
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(MusicPlayerEventHandler::onClientKeyboard);
		HudRenderCallback.EVENT.register(MusicPlayerEventHandler::onRenderGameOverlay);
		ScreenEvents.BEFORE_INIT.register(MusicPlayerEventHandler::onPreInitScreen);
		ScreenEvents.AFTER_INIT.register(MusicPlayerEventHandler::onPostInitScreen);
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			ScreenKeyboardEvents.afterKeyPress(screen).register(MusicPlayerEventHandler::onScreenKeyboard);
			if (screen instanceof PauseScreen) {
				ScreenEvents.afterRender(screen).register(MusicPlayerEventHandler::onDrawScreenPost);
				ScreenMouseEvents.beforeMouseRelease(screen).register(MusicPlayerEventHandler::onMouseReleasePre);
				ScreenEvents.afterTick(screen).register(MusicPlayerEventHandler::onScreenTick);
			}
		});
	}
}
