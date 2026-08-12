package info.u_team.music_player.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import info.u_team.music_player.audio.AudioDuckingController;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

/** Minecraft 1.21.9-1.21.11: SoundManager.play returns PlayResult. */
@Mixin(SoundManager.class)
abstract class SoundManagerMixin {
	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"))
	private void musicplayer$noticeImportantSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> callback) {
		AudioDuckingController.onSound(sound.getSource());
	}
}
