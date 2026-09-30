package info.u_team.music_player.gui;

import java.net.URI;

import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.lavaplayer.api.IYoutubeAuth.DeviceCode;
import info.u_team.music_player.musicplayer.YoutubeLoginCoordinator;
import info.u_team.music_player.musicplayer.YoutubeLoginCoordinator.State;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiYoutubeLogin extends BetterScreen {

	private final Screen previousScreen;
	private UButton loginButton;
	private boolean browserOpened;
	private String browserError;

	public GuiYoutubeLogin(Screen previousScreen) {
		super(Component.literal("Sign in to YouTube"));
		this.previousScreen = previousScreen;
	}

	@Override
	protected void init() {
		final int buttonY = height / 2 + 55;
		final int leftWidth = Math.min(220, Math.max(100, width / 2 - 24));
		final int rightWidth = Math.min(100, Math.max(70, width / 3));
		final int leftX = (width - leftWidth - rightWidth - 10) / 2;
		addRenderableWidget(new UButton(leftX, buttonY, leftWidth, 20,
				Component.literal("Skip (YouTube will not work)"), button -> onClose()));
		loginButton = addRenderableWidget(new UButton(leftX + leftWidth + 10, buttonY, rightWidth, 20,
				Component.literal("Sign in"), button -> {
					if (YoutubeLoginCoordinator.getState() == State.WAITING) {
						openBrowser();
					} else {
						YoutubeLoginCoordinator.beginLogin();
					}
				}));
	}

	@Override
	public void tick() {
		final State state = YoutubeLoginCoordinator.getState();
		if (state == State.AUTHENTICATED) {
			minecraft.gui.setScreen(previousScreen);
			return;
		}
		loginButton.active = state == State.NEEDS_LOGIN || state == State.ERROR || state == State.WAITING;
		loginButton.setMessage(Component.literal(state == State.WAITING ? "Open browser" : "Sign in"));
		if (state == State.WAITING && YoutubeLoginCoordinator.getCode() != null && !browserOpened) {
			browserOpened = true;
			openBrowser();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		graphics.centeredText(font, "Sign in with Google to listen through YouTube", width / 2, height / 2 - 61, 0xFFFFFFFF);
		graphics.centeredText(font, "Your Google account may be blocked", width / 2, height / 2 - 45, 0xFFFF7777);
		final State state = YoutubeLoginCoordinator.getState();
		if (state == State.REQUESTING || state == State.RESTORING) {
			graphics.centeredText(font, "Getting a sign-in code...", width / 2, height / 2 - 12, 0xFFFFFFFF);
		} else if (state == State.WAITING) {
			final DeviceCode code = YoutubeLoginCoordinator.getCode();
			if (code != null) {
				graphics.centeredText(font, "Open this page and enter the code:", width / 2, height / 2 - 17, 0xFFFFFFFF);
				graphics.centeredText(font, code.verificationUrl(), width / 2, height / 2 - 2, 0xFF88CCFF);
				graphics.centeredText(font, code.userCode(), width / 2, height / 2 + 14, 0xFFFFFF55);
				graphics.centeredText(font, "Waiting for browser confirmation...", width / 2, height / 2 + 33, 0xFFFFFFFF);
			}
		} else if (state == State.ERROR) {
			graphics.centeredText(font, YoutubeLoginCoordinator.getError(), width / 2, height / 2 - 10, 0xFFFF7777);
		}
		if (browserError != null) {
			graphics.centeredText(font, browserError, width / 2, height / 2 + 43, 0xFFFF7777);
		}
	}

	private void openBrowser() {
		final DeviceCode code = YoutubeLoginCoordinator.getCode();
		if (code == null) {
			return;
		}
		try {
			final URI uri = URI.create(code.verificationUrl());
			final String host = uri.getHost();
			if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
					|| !(host.equalsIgnoreCase("google.com") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".google.com"))) {
				throw new IllegalArgumentException("Unexpected verification URL");
			}
			Util.getPlatform().openUri(uri);
			browserError = null;
		} catch (RuntimeException ex) {
			browserError = "Could not open the browser. Open the page manually.";
		}
	}

	@Override
	public void onClose() {
		YoutubeLoginCoordinator.skip();
		minecraft.gui.setScreen(previousScreen);
	}
}
