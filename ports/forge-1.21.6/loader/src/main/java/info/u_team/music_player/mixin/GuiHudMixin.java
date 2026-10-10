package info.u_team.music_player.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import info.u_team.music_player.init.MusicPlayerClientConstruct;
import info.u_team.music_player.init.MusicPlayerEventHandler;

// Forge 56/57 provide no HUD layer registration event.
@Mixin(value = Gui.class, remap = false)
public abstract class GuiHudMixin {
    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void musicplayer$renderHud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo callback) {
        if (MusicPlayerClientConstruct.isReady()) MusicPlayerEventHandler.onRenderGameOverlay(graphics, delta);
    }
}
