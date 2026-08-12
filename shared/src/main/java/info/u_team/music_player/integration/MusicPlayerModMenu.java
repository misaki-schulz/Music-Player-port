package info.u_team.music_player.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import info.u_team.music_player.gui.settings.GuiMusicPlayerAdvancedSettings;

/**
 * The single Mod Menu integration point. Advanced settings will live behind this entrypoint so the player screen does
 * not grow a second copy of the complete configuration UI.
 */
public final class MusicPlayerModMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return GuiMusicPlayerAdvancedSettings::new;
	}
}
