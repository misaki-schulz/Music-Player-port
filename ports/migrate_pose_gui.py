"""Backport the 1.21.6 GUI pose and HUD changes to 1.21.2–1.21.5."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent
GROUPS = ('1.21.2-1.21.3', '1.21.4', '1.21.5')


def main() -> None:
    for group in GROUPS:
        changed = 0
        for base in (ROOT / group / 'common' / 'src', ROOT / group / 'fabric' / 'src'):
            for file in base.rglob('*.java'):
                old = file.read_text(encoding='utf-8')
                source = old.replace('import org.joml.Matrix3x2fStack;',
                                     'import com.mojang.blaze3d.vertex.PoseStack;')
                source = source.replace('Matrix3x2fStack', 'PoseStack')
                source = source.replace('.pushMatrix()', '.pushPose()')
                source = source.replace('.popMatrix()', '.popPose()')
                source = source.replace('.identity()', '.setIdentity()')
                source = source.replace('pose.scale(scale, scale)', 'pose.scale(scale, scale, 1)')
                source = source.replace('poseStack.scale(scale, scale)', 'poseStack.scale(scale, scale, 1)')
                source = source.replace('poseStack.translate(x, y)', 'poseStack.translate(x, y, 0)')
                if file.name == 'UButton.java':
                    source = source.replace('net.minecraft.client.renderer.RenderPipelines',
                                            'net.minecraft.client.renderer.RenderType')
                    source = source.replace('RenderPipelines.GUI_TEXTURED, SPRITES.get',
                                            'RenderType::guiTextured, SPRITES.get')
                if file.name == 'ImageButton.java':
                    if 'import net.minecraft.client.renderer.RenderType;' not in source:
                        source = source.replace('import net.minecraft.network.chat.Component;',
                                                'import net.minecraft.client.renderer.RenderType;\n'
                                                'import net.minecraft.network.chat.Component;')
                    source = source.replace('graphics.blit(currentImage,\n'
                                            '\t\t\t\tgetX() + 2, getY() + 2,\n'
                                            '\t\t\t\tgetX() + getWidth() - 2, getY() + getHeight() - 2,\n'
                                            '\t\t\t\t0, 1, 0, 1);',
                                            'graphics.blit(RenderType::guiTextured, currentImage,\n'
                                            '\t\t\t\tgetX() + 2, getY() + 2, 0, 0,\n'
                                            '\t\t\t\tgetWidth() - 4, getHeight() - 4,\n'
                                            '\t\t\t\tgetWidth() - 4, getHeight() - 4);')
                if file.name == 'MusicPlayerEventHandler.java':
                    source = source.replace('net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry',
                                            'net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback')
                    source = source.replace('HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("musicplayer", "player_overlay"), MusicPlayerEventHandler::onRenderGameOverlay);',
                                            'HudRenderCallback.EVENT.register(MusicPlayerEventHandler::onRenderGameOverlay);')
                if source != old:
                    file.write_text(source, encoding='utf-8', newline='')
                    changed += 1
        print(f'{group}: updated {changed} files')


if __name__ == '__main__':
    main()
