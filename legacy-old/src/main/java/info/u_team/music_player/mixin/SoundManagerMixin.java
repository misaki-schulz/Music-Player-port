package info.u_team.music_player.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import info.u_team.music_player.audio.AudioDuckingController;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;

/** Minecraft 1.21.2-1.21.8: SoundManager.play returns void. */
@Mixin(SoundManager.class)
abstract class SoundManagerMixin {
	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), require = 0)
	private void musicplayer$noticeImportantSound(SoundInstance sound, CallbackInfo callback) {
		AudioDuckingController.onSound(sound.getSource());
	}
}
