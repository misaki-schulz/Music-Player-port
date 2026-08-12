// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.init;

import java.util.List;

import com.mojang.blaze3d.platform.Window;
import org.joml.Matrix3x2fStack;

import info.u_team.music_player.gui.GuiMusicPlayer;
import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.MusicPlayerUtils;
import info.u_team.music_player.musicplayer.SettingsManager;
import info.u_team.music_player.musicplayer.settings.IngameOverlayPosition;
import info.u_team.music_player.render.RenderOverlayMusicDisplay;
import info.u_team.music_player.util.MinecraftGuiCompat;
import info.u_team.music_player.gui.widget.ScrollingText;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public class MusicPlayerEventHandler {
	
	private static final SettingsManager settingsManager = MusicPlayerManager.getSettingsManager();
	
	// Used to listen to keyboard events
	
	public static void onKeyInput() {
		handleKeyboard(false, null);
	}
	
	public static boolean onKeyboardPressed(Screen screen, KeyEvent event) {
		// The player menu must always remain reachable, including from the title,
		// pause and mod menus. The GUI hotkey setting only controls playback keys.
		if (isKeyDown(MusicPlayerKeys.OPEN, true, event)) {
			openMusicPlayer();
			return true;
		}
		if (settingsManager.getSettings().isKeyWorkInGui()) {
			return handlePlaybackKeyboard(true, event);
		}
		return false;
	}
	
	private static boolean handleKeyboard(boolean gui, KeyEvent event) {
		if (isKeyDown(MusicPlayerKeys.OPEN, gui, event)) {
			openMusicPlayer();
			return true;
		}
		return handlePlaybackKeyboard(gui, event);
	}

	private static boolean handlePlaybackKeyboard(boolean gui, KeyEvent event) {
		final ITrackManager manager = MusicPlayerManager.getPlayer().getTrackManager();
		if (isKeyDown(MusicPlayerKeys.PAUSE, gui, event)) {
			if (manager.getCurrentTrack() != null) {
				manager.setPaused(!manager.isPaused());
			}
			return true;
		} else if (isKeyDown(MusicPlayerKeys.SKIP_FORWARD, gui, event)) {
			if (manager.getCurrentTrack() != null) {
				MusicPlayerUtils.skipForward();
			}
			return true;
		} else if (isKeyDown(MusicPlayerKeys.SKIP_BACK, gui, event)) {
			if (manager.getCurrentTrack() != null) {
				MusicPlayerUtils.skipBack();
			}
			return true;
		}
		return false;
	}

	private static void openMusicPlayer() {
		final Minecraft mc = Minecraft.getInstance();
		if (!(MinecraftGuiCompat.getScreen(mc) instanceof GuiMusicPlayer)) {
			MinecraftGuiCompat.setScreen(mc, new GuiMusicPlayer());
		}
	}

	private static boolean isKeyDown(KeyMapping binding, boolean gui, KeyEvent event) {
		return gui ? binding.matches(event) : binding.consumeClick();
	}

	private static RenderOverlayMusicDisplay overlayRender;
	
	// Render overlay
	
	public static void onRenderGameOverlay(GuiGraphicsExtractor guiGraphics, DeltaTracker partialTick) {
		final Minecraft mc = Minecraft.getInstance();
		if (MinecraftGuiCompat.getScreen(mc) == null) {
			if (settingsManager.getSettings().isShowIngameOverlay()) {
				final IngameOverlayPosition position = settingsManager.getSettings().getIngameOverlayPosition();
				
				if (overlayRender == null) {
					overlayRender = new RenderOverlayMusicDisplay();
				}
				
				final Window window = mc.getWindow();
				final int screenWidth = window.getGuiScaledWidth();
				final int screenHeight = window.getGuiScaledHeight();
				
				final int baseHeight = overlayRender.getHeight();
				final int baseWidth = overlayRender.getWidth();
				final float requestedScale = settingsManager.getSettings().getOverlayScale();
				final float fitScale = Math.min((screenWidth - 6F) / baseWidth, (screenHeight - 6F) / baseHeight);
				final float scale = Math.max(0.1F, Math.min(requestedScale, fitScale));
				final int height = Math.round(baseHeight * scale);
				final int width = Math.round(baseWidth * scale);
				
				final int x;
				if (position.isLeft()) {
					x = 3;
				} else {
					x = screenWidth - 3 - width;
				}
				
				final int y;
				if (position.isUp()) {
					y = 3;
				} else {
					y = screenHeight - 3 - height;
				}
				
				final Matrix3x2fStack poseStack = guiGraphics.pose();
				
				poseStack.pushMatrix();
				poseStack.translate(x, y);
				poseStack.scale(scale, scale);
				overlayRender.extractRenderState(guiGraphics, 0, 0, partialTick.getGameTimeDeltaPartialTick(false));
				poseStack.popMatrix();
			}
		}
	}
	
	// Used to add buttons and gui controls to main ingame gui
	
	private static ScrollingText titleRender, authorRender;
	
	private static void onPreInitScreen(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof PauseScreen) {
			if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
				screen.children().stream() //
						.filter(element -> element instanceof GuiControls) //
						.map(element -> ((GuiControls) element)).findAny() //
						.ifPresent(controls -> {
							titleRender = controls.getTitleRender();
							authorRender = controls.getAuthorRender();
						});
			}
		}
	}
	
	private static void onPostInitScreen(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof PauseScreen) {
			if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
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
				final List<GuiEventListener> list = (List<GuiEventListener>) screen.children();
				list.add(controls);
			}
		}
	}
	
	private static void onDrawScreenPost(Screen screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
			screen.children().stream() //
					.filter(element -> element instanceof GuiControls) //
					.map(element -> ((GuiControls) element)).findAny() //
					.ifPresent(controls -> controls.extractRenderState(guiGraphics, mouseX, mouseY, partialTick));
		}
	}
	
	private static void onMouseReleasePre(Screen screen, MouseButtonEvent event) {
		if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
			screen.children().stream() //
					.filter(element -> element instanceof GuiControls) //
					.map(element -> ((GuiControls) element)).findAny() //
					.ifPresent(controls -> controls.mouseReleased(event));
		}
	}
	
	private static void onClientTick(Screen screen) {
		if (settingsManager.getSettings().isShowIngameMenueOverlay()) {
			screen.children().stream() //
					.filter(element -> element instanceof GuiControls) //
					.map(element -> ((GuiControls) element)).findAny() //
					.ifPresent(GuiControls::tick);
		}
	}
	
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (MinecraftGuiCompat.getScreen(client) == null) {
				onKeyInput();
			}
		});
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("musicplayer", "player_overlay"), MusicPlayerEventHandler::onRenderGameOverlay);

		ScreenEvents.BEFORE_INIT.register(MusicPlayerEventHandler::onPreInitScreen);
		ScreenEvents.AFTER_INIT.register(MusicPlayerEventHandler::onPostInitScreen);

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			ScreenKeyboardEvents.afterKeyPress(screen).register((currentScreen, event) -> onKeyboardPressed(currentScreen, event));
			if (screen instanceof PauseScreen) {
				ScreenEvents.afterExtract(screen).register(MusicPlayerEventHandler::onDrawScreenPost);
				ScreenMouseEvents.beforeMouseRelease(screen).register(MusicPlayerEventHandler::onMouseReleasePre);
				ScreenEvents.afterTick(screen).register(MusicPlayerEventHandler::onClientTick);
			}
		});
	}
	
}
