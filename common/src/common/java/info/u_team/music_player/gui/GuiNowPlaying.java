package info.u_team.music_player.gui;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.artwork.ArtworkRenderer;
import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.gui.history.GuiLyrics;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.widget.USlider;
import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.ABRepeatManager;
import info.u_team.music_player.musicplayer.LibraryStateManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.MusicPlayerUtils;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.musicplayer.TrackRadioService;
import info.u_team.music_player.gui.playlist.TrackCardDialogs;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Full-size current-track view. Persistent configuration remains in Mod Menu; this screen contains playback actions. */
public final class GuiNowPlaying extends BetterScreen {

	private final Screen previous;
	private final Page page;
	private int artworkX;
	private int artworkY;
	private int artworkSize;
	private UButton title;
	private UButton metadata;
	private UButton position;
	private UButton pause;
	private UButton favorite;
	private UButton loopStatus;
	private UButton actionStatus;
	private final UButton[] ratings = new UButton[5];

	public GuiNowPlaying(Screen previous) {
		this(previous, Page.PLAYBACK);
	}

	private GuiNowPlaying(Screen previous, Page page) {
		super(Component.literal(getTranslation("gui.now.title")));
		this.previous = previous;
		this.page = page;
	}

	@Override
	protected void init() {
		addRenderableWidget(new UButton(8, 8, 28, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previous)));
		title = disabled(40, 8, width - 48, 20, trackTitle());
		artworkX = 16;
		artworkY = 34;
		artworkSize = Math.max(72, Math.min(180, Math.min(width / 2 - 28, height - 48)));
		final int rightX = artworkX + artworkSize + 12;
		final int rightWidth = Math.max(80, width - rightX - 12);
		metadata = disabled(rightX, 34, rightWidth, 18, trackMetadata());
		position = disabled(rightX, 56, rightWidth, 18, positionLabel());

		final int actionWidth = Math.max(26, (rightWidth - 16) / 5);
		addAction(rightX, 78, actionWidth, "<", MusicPlayerUtils::skipBack);
		pause = addAction(rightX + actionWidth + 4, 78, actionWidth, pauseLabel(), this::togglePause);
		addAction(rightX + (actionWidth + 4) * 2, 78, actionWidth, ">", MusicPlayerUtils::skipForward);
		favorite = addAction(rightX + (actionWidth + 4) * 3, 78, actionWidth, favoriteLabel(), this::toggleFavorite);
		addAction(rightX + (actionWidth + 4) * 4, 78, actionWidth, "URI", () -> {
			final IPlayingTrack track = track();
			if (track != null) GuiTrackUtils.openURI(track.getInfo().getURI());
		});

		final int half = (rightWidth - 4) / 2;
		final UButton playbackTab = addAction(rightX, 100, half, getTranslation("gui.now.playback"), () -> openPage(Page.PLAYBACK));
		final UButton toolsTab = addAction(rightX + half + 4, 100, half, getTranslation("gui.now.tools"), () -> openPage(Page.TOOLS));
		playbackTab.active = page != Page.PLAYBACK;
		toolsTab.active = page != Page.TOOLS;
		if (page == Page.PLAYBACK) initPlaybackPage(rightX, rightWidth);
		else initToolsPage(rightX, rightWidth);
		refreshRating();
	}

	private void initPlaybackPage(int rightX, int rightWidth) {
		final int third = Math.max(42, (rightWidth - 8) / 3);
		addAction(rightX, 122, third, getTranslation("gui.now.set_a"), this::setPointA);
		addAction(rightX + third + 4, 122, third, getTranslation("gui.now.set_b"), this::setPointB);
		addAction(rightX + (third + 4) * 2, 122, third, getTranslation("gui.now.clear_ab"), () -> { MusicPlayerManager.getABRepeatManager().clear(); refreshLoop(); });
		loopStatus = disabled(rightX, 144, rightWidth, 18, loopLabel());
		final int ratingWidth = Math.max(24, (rightWidth - 16) / 5);
		for (int index = 0; index < ratings.length; index++) { final int rating = index + 1; ratings[index] = addAction(rightX + index * (ratingWidth + 4), 166, ratingWidth, "" + rating, () -> setRating(rating)); }
		final IMusicPlayer player = MusicPlayerManager.getPlayer();
		addRenderableWidget(new USlider(rightX, 188, rightWidth, 20, Component.literal(getTranslation("gui.now.track_speed") + ": "), Component.literal("x"), Settings.MIN_SPEED, Settings.MAX_SPEED, player.getSpeed(), true, true, slider -> saveTrackPlayback((float) slider.getValue(), player.getPitch()))).setPrecision(2);
		addRenderableWidget(new USlider(rightX, 210, rightWidth, 20, Component.literal(getTranslation("gui.now.track_pitch") + ": "), Component.literal("x"), Settings.MIN_PITCH, Settings.MAX_PITCH, player.getPitch(), true, true, slider -> saveTrackPlayback(player.getSpeed(), (float) slider.getValue()))).setPrecision(2);
	}

	private void initToolsPage(int rightX, int rightWidth) {
		addAction(rightX, 122, rightWidth, getTranslation("gui.now.use_global"), this::clearTrackPlayback);
		addAction(rightX, 144, rightWidth, getTranslation("gui.now.open_lyrics"), () -> MinecraftGuiCompat.setScreen(minecraft, new GuiLyrics(this)));
		addAction(rightX, 166, rightWidth, getTranslation("gui.now.track_radio"), () -> TrackRadioService.queueRecommendations(10, this::setActionStatus));
		final int half = (rightWidth - 4) / 2;
		addAction(rightX, 188, half, getTranslation("gui.now.export_card"), () -> TrackCardDialogs.exportCard(track(), this::setActionStatus));
		addAction(rightX + half + 4, 188, half, getTranslation("gui.now.export_qr"), () -> TrackCardDialogs.exportQr(track(), this::setActionStatus));
		actionStatus = disabled(rightX, 210, rightWidth, 18, "");
	}

	private void openPage(Page target) { if (target != page) MinecraftGuiCompat.setScreen(minecraft, new GuiNowPlaying(previous, target)); }

	private UButton disabled(int x, int y, int width, int height, String label) {
		final UButton button = addRenderableWidget(new UButton(x, y, width, height, Component.literal(label)));
		button.active = false;
		return button;
	}

	private UButton addAction(int x, int y, int width, String label, Runnable action) {
		final UButton button = addRenderableWidget(new UButton(x, y, width, 20, Component.literal(label)));
		button.setPressable(action);
		return button;
	}

	@Override
	public void tick() {
		if (track() == null) return;
		title.setMessage(Component.literal(trackTitle()));
		metadata.setMessage(Component.literal(trackMetadata()));
		position.setMessage(Component.literal(positionLabel()));
		pause.setMessage(Component.literal(pauseLabel()));
		favorite.setMessage(Component.literal(favoriteLabel()));
		refreshLoop();
		refreshRating();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		final IPlayingTrack track = track();
		if (track != null) {
			ArtworkRenderer.render(graphics, track, artworkX, artworkY, artworkSize);
			final float[] spectrum = MusicPlayerManager.getAudioVisualizer().spectrum();
			for (int index = 0; index < spectrum.length; index++) {
				final int left = artworkX + index * artworkSize / spectrum.length;
				final int bar = Math.max(1, Math.round(spectrum[index] * 28F));
				graphics.fill(left, artworkY + artworkSize - bar, left + Math.max(1, artworkSize / spectrum.length - 1), artworkY + artworkSize, 0xB075E0B5);
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){super.extractBackground(graphics,mouseX,mouseY,partialTick);if(MusicPlayerManager.getSettingsManager().getSettings().isDynamicTheme()){float energy=0F;for(final float value:MusicPlayerManager.getAudioVisualizer().spectrum())energy+=value;final int alpha=Math.clamp(35+Math.round(energy*3F),35,105);graphics.fill(0,0,width,height,alpha<<24|0x00305042);}}

	private void togglePause() { final var manager = MusicPlayerManager.getPlayer().getTrackManager(); if (manager.getCurrentTrack() != null) manager.setPaused(!manager.isPaused()); }
	private void toggleFavorite() { final IPlayingTrack track = track(); if (track != null) MusicPlayerManager.getLibraryStateManager().toggleFavorite(track.getInfo()); }
	private void setPointA() { MusicPlayerManager.getABRepeatManager().setA(MusicPlayerManager.getPlayer()); refreshLoop(); }
	private void setPointB() { MusicPlayerManager.getABRepeatManager().setB(MusicPlayerManager.getPlayer()); refreshLoop(); }
	private void refreshLoop() { if (loopStatus != null) loopStatus.setMessage(Component.literal(loopLabel())); }
	private void setRating(int rating) { final IPlayingTrack track=track(); if(track==null)return; final LibraryStateManager library=MusicPlayerManager.getLibraryStateManager(); library.setRating(track.getInfo(),library.getRating(track.getInfo())==rating?0:rating);refreshRating(); }
	private void refreshRating(){final IPlayingTrack track=track();final int selected=track==null?0:MusicPlayerManager.getLibraryStateManager().getRating(track.getInfo());for(int i=0;i<ratings.length;i++)if(ratings[i]!=null)ratings[i].setMessage(Component.literal((i+1)+(selected==i+1?"*":"")));}
	private void saveTrackPlayback(float speed,float pitch){final IPlayingTrack track=track();if(track==null)return;final IMusicPlayer player=MusicPlayerManager.getPlayer();player.setSpeed(speed);player.setPitch(pitch);MusicPlayerManager.getLibraryStateManager().setTrackPlaybackPreferences(track.getInfo(),speed,pitch);}
	private void clearTrackPlayback(){final IPlayingTrack track=track();if(track!=null)MusicPlayerManager.getLibraryStateManager().clearTrackPlaybackPreferences(track.getInfo(),MusicPlayerManager.getPlayer(),MusicPlayerManager.getSettingsManager().getSettings());}
	private void setActionStatus(String value){if(actionStatus!=null)actionStatus.setMessage(Component.literal(value));}
	private IPlayingTrack track(){return MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack();}
	private String trackTitle(){final IPlayingTrack t=track();return t==null?getTranslation("gui.now.nothing"):t.getInfo().getFixedTitle();}
	private String trackMetadata(){final IPlayingTrack t=track();return t==null?"":t.getInfo().getFixedAuthor()+"  |  "+sourceLabel(t.getInfo().getURI());}
	private String positionLabel(){final IPlayingTrack t=track();return t==null?"":GuiTrackUtils.getFormattedPosition(t)+" / "+GuiTrackUtils.getFormattedDuration(t);}
	private String pauseLabel(){return getTranslation(MusicPlayerManager.getPlayer().getTrackManager().isPaused()?"gui.now.play":"gui.now.pause");}
	private String favoriteLabel(){final IPlayingTrack t=track();return getTranslation(t!=null&&MusicPlayerManager.getLibraryStateManager().isFavorite(t.getInfo())?"gui.now.favorite_marked":"gui.now.favorite");}
	private String loopLabel(){final ABRepeatManager loop=MusicPlayerManager.getABRepeatManager();return "A: "+format(loop.getPointA())+"   B: "+format(loop.getPointB())+(loop.isActive()?"   "+getTranslation("gui.now.looping"):"");}
	private static String format(long millis){return millis<0?"--:--":info.u_team.music_player.util.TimeUtil.timeConversion(millis/1000L);}
	private static String sourceLabel(String uri){if(uri==null||uri.isBlank())return getTranslation("gui.now.unknown_source");try{final String host=java.net.URI.create(uri).getHost();return host==null?getTranslation("gui.now.local_source"):host;}catch(final IllegalArgumentException ignored){return getTranslation("gui.now.local_source");}}
	private enum Page { PLAYBACK, TOOLS }
}
