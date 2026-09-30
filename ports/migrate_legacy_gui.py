"""Apply the mechanical 26.2 -> obfuscated-1.21 client GUI renames.

API signatures still vary by release; compile each profile after this pass.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parent
GROUPS = {
    '1.21.2-1.21.3', '1.21.4', '1.21.5',
    '1.21.6-1.21.8', '1.21.9-1.21.10', '1.21.11',
}
OLD_KEY_GROUPS = GROUPS - {'1.21.9-1.21.10', '1.21.11'}
OLD_GUI_GROUPS = GROUPS - {'1.21.11'}


def migrate(text: str) -> str:
    for old, new in (
        ('GuiGraphicsExtractor', 'GuiGraphics'),
        ('extractRenderState', 'render'),
        ('extractBackground', 'renderBackground'),
        ('extractContents', 'renderContents'),
        ('extractListItems', 'renderListItems'),
        ('extractContent', 'renderContent'),
        ('extractWidgetRenderState', 'renderWidget'),
        ('extractScrollingStringOverContents', 'renderScrollingStringOverContents'),
        ('.gui.setScreen(', '.setScreen('),
        ('.gui.screen()', '.screen'),
        ('ScreenEvents.afterExtract(', 'ScreenEvents.afterRender('),
        ('net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper',
         'net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper'),
        ('KeyMappingHelper.registerKeyMapping(', 'KeyBindingHelper.registerKeyBinding('),
        ('.centeredText(', '.drawCenteredString('),
    ):
        text = text.replace(old, new)
    text = re.sub(r'\b(guiGraphics|graphics|context|gui)\.text\(', r'\1.drawString(', text)
    text = text.replace(
        '\tpublic void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {\n'
        '\t\trender(graphics, mouseX, mouseY, partialTick);\n\t}\n\n',
        '',
    )
    return text


def main() -> None:
    selected = sys.argv[1:] or sorted(GROUPS)
    if any(group not in GROUPS for group in selected):
        raise SystemExit(f'Choose groups from {sorted(GROUPS)}')
    for group in selected:
        count = 0
        for base in (ROOT / group / 'common' / 'src', ROOT / group / 'fabric' / 'src'):
            for file in base.rglob('*.java'):
                original = file.read_text(encoding='utf-8')
                changed = migrate(original)
                if group != '1.21.11':
                    changed = re.sub(r'\bIdentifier\b', 'ResourceLocation', changed)
                if group in OLD_GUI_GROUPS:
                    changed = changed.replace('net.minecraft.util.Util', 'net.minecraft.Util')
                    changed = changed.replace('renderContents(', 'renderWidget(')
                    if file.name in {
                        'GuiMusicPlayer.java', 'GuiMusicPlaylist.java',
                        'GuiMusicSearch.java', 'GuiMusicPlayerSettings.java',
                    }:
                        changed = changed.replace(
                            'public void resize(int width, int height)',
                            'public void resize(Minecraft minecraft, int width, int height)',
                        )
                        changed = changed.replace('init(width, height);', 'init(minecraft, width, height);')
                        if ('resize(Minecraft minecraft' in changed
                                and 'import net.minecraft.client.Minecraft;' not in changed):
                            changed = changed.replace(
                                'import net.minecraft.client.gui.GuiGraphics;',
                                'import net.minecraft.client.Minecraft;\nimport net.minecraft.client.gui.GuiGraphics;',
                            )
                    if file.name == 'UButton.java':
                        changed = changed.replace('getAlpha()', 'alpha')
                        if 'import net.minecraft.client.Minecraft;' not in changed:
                            changed = changed.replace(
                                'import net.minecraft.client.gui.GuiGraphics;',
                                'import net.minecraft.client.Minecraft;\nimport net.minecraft.client.gui.GuiGraphics;',
                            )
                        changed = re.sub(
                            r'renderScrollingStringOverContents\(\s*'
                            r'graphics\.textRendererForWidget\(this, GuiGraphics\.HoveredTextEffects\.NONE\),\s*'
                            r'label,\s*2\);',
                            'renderScrollingString(graphics, Minecraft.getInstance().font, label, '
                            'getX() + 2, getY(), getRight() - 2, getBottom(), '
                            'foreground.getColorARGB(alpha));',
                            changed,
                        )
                    if file.name == 'USlider.java':
                        changed = changed.replace(
                            'setValue(Math.clamp((currentValue - minValue) / (maxValue - minValue), 0, 1));',
                            'final double normalized = Math.clamp((currentValue - minValue) / (maxValue - minValue), 0, 1);\n'
                            '\t\tif (value != normalized) {\n'
                            '\t\t\tvalue = normalized;\n'
                            '\t\t\tupdateMessage();\n'
                            '\t\t\tapplyValue();\n'
                            '\t\t}',
                        )
                if group in OLD_KEY_GROUPS and file.name == 'MusicPlayerKeys.java':
                    if 'import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_CATEGORY;' not in changed:
                        changed = changed.replace(
                            'import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_OPEN;',
                            'import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_CATEGORY;\n'
                            'import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_OPEN;',
                        )
                    changed = changed.replace(
                        'private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register('
                        'ResourceLocation.fromNamespaceAndPath("musicplayer", "main"));',
                        'private static final String CATEGORY = KEY_CATEGORY;',
                    )
                    changed = changed.replace(
                        'new KeyMapping(KEY_OPEN, GLFW.GLFW_KEY_F8, CATEGORY)',
                        'new KeyMapping(KEY_OPEN, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY)',
                    )
                if changed != original:
                    file.write_text(changed, encoding='utf-8', newline='')
                    count += 1
        print(f'{group}: updated {count} Java files')


if __name__ == '__main__':
    main()
