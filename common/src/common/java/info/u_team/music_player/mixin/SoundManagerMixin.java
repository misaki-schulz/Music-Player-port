package info.u_team.music_player.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import info.u_team.music_player.audio.AudioDuckingController;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

/** Minecraft 26.x: SoundManager.play returns PlayResult. */
@Mixin(SoundManager.class)
abstract class SoundManagerMixin {
	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"))
	private void musicplayer$noticeImportantSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> callback) {
		// At HEAD, Minecraft has not necessarily resolved the event to a concrete Sound yet.
		// AbstractSoundInstance#getVolume dereferences that nullable Sound on 26.x.
		AudioDuckingController.onSound(sound.getSource());
	}
}
