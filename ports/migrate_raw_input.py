"""Backport 26.2 event objects to Minecraft's pre-1.21.9 primitive input API."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
GROUPS = ('1.21.2-1.21.3', '1.21.4', '1.21.5', '1.21.6-1.21.8')


def migrate(source: str, name: str) -> str:
    source = source.replace('import net.minecraft.client.input.MouseButtonEvent;\n', '')
    source = source.replace('import net.minecraft.client.input.KeyEvent;\n', '')
    source = source.replace('import net.minecraft.client.input.InputWithModifiers;\n', '')
    source = source.replace('net.minecraft.client.input.MouseButtonEvent', 'double')
    source = source.replace('mouseClicked(MouseButtonEvent event, boolean doubleClick)',
                            'mouseClicked(double mouseX, double mouseY, int button)')
    source = source.replace('mouseReleased(MouseButtonEvent event)',
                            'mouseReleased(double mouseX, double mouseY, int button)')
    source = source.replace('mouseDragged(MouseButtonEvent event, double dragX, double dragY)',
                            'mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)')
    source = source.replace('onClick(MouseButtonEvent event, boolean doubleClick)',
                            'onClick(double mouseX, double mouseY)')
    source = source.replace('onRelease(MouseButtonEvent event)',
                            'onRelease(double mouseX, double mouseY)')
    source = source.replace('onDrag(MouseButtonEvent event, double dragX, double dragY)',
                            'onDrag(double mouseX, double mouseY, double dragX, double dragY)')
    source = source.replace('mouseClicked(double event, boolean doubleClick)',
                            'mouseClicked(double mouseX, double mouseY, int button)')
    source = source.replace('event.button()', 'button')
    source = source.replace('event.x()', 'mouseX')
    source = source.replace('event.y()', 'mouseY')
    source = source.replace('mouseClicked(event, doubleClick)', 'mouseClicked(mouseX, mouseY, button)')
    source = source.replace('mouseReleased(event)', 'mouseReleased(mouseX, mouseY, button)')
    source = source.replace('mouseDragged(event, dragX, dragY)',
                            'mouseDragged(mouseX, mouseY, button, dragX, dragY)')
    source = source.replace('onClick(event, doubleClick)', 'onClick(mouseX, mouseY)')
    source = source.replace('onRelease(event)', 'onRelease(mouseX, mouseY)')
    source = source.replace('onPress(InputWithModifiers input)', 'onPress()')
    source = source.replace('super.onPress(input)', 'super.onPress()')
    if name == 'GuiMusicSearch.java':
        source = source.replace('keyPressed(KeyEvent event)',
                                'keyPressed(int key, int scancode, int modifiers)')
        source = source.replace('event.key()', 'key')
        source = source.replace('super.keyPressed(event)', 'super.keyPressed(key, scancode, modifiers)')
    if name == 'ScrollableListEntry.java':
        source = source.replace(
            '\t@Override\n\tpublic final void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {\n'
            '\t\trender(graphics, -1, getY(), getX(), getWidth(), getHeight(), mouseX, mouseY, hovered, partialTick);\n\t}\n\n', '')
        source = source.replace('\t@Override\n\tpublic void visitWidgets(', '\tpublic void visitWidgets(')
    if name == 'MusicPlayerEventHandler.java':
        source = source.replace('onKeyboardPressed(Screen screen, KeyEvent event)',
                                'onKeyboardPressed(Screen screen, int key, int scancode, int modifiers)')
        source = source.replace('handleKeyboard(boolean gui, KeyEvent event)',
                                'handleKeyboard(boolean gui, int key, int scancode, int modifiers)')
        source = source.replace('handlePlaybackKeyboard(boolean gui, KeyEvent event)',
                                'handlePlaybackKeyboard(boolean gui, int key, int scancode, int modifiers)')
        source = source.replace('isKeyDown(KeyMapping binding, boolean gui, KeyEvent event)',
                                'isKeyDown(KeyMapping binding, boolean gui, int key, int scancode)')
        source = source.replace('handleKeyboard(false, null)', 'handleKeyboard(false, 0, 0, 0)')
        source = source.replace('binding.matches(event)', 'binding.matches(key, scancode)')
        source = source.replace('isKeyDown(MusicPlayerKeys.OPEN, true, event)',
                                'isKeyDown(MusicPlayerKeys.OPEN, true, key, scancode)')
        source = source.replace('isKeyDown(MusicPlayerKeys.OPEN, gui, event)',
                                'isKeyDown(MusicPlayerKeys.OPEN, gui, key, scancode)')
        source = re.sub(r'isKeyDown\((MusicPlayerKeys\.[A-Z_]+), gui, event\)',
                        r'isKeyDown(\1, gui, key, scancode)', source)
        source = source.replace('handlePlaybackKeyboard(true, event)',
                                'handlePlaybackKeyboard(true, key, scancode, modifiers)')
        source = source.replace('handlePlaybackKeyboard(gui, event)',
                                'handlePlaybackKeyboard(gui, key, scancode, modifiers)')
        source = source.replace('onMouseReleasePre(Screen screen, MouseButtonEvent event)',
                                'onMouseReleasePre(Screen screen, double mouseX, double mouseY, int button)')
        source = source.replace('(currentScreen, event) -> onKeyboardPressed(currentScreen, event)',
                                '(currentScreen, key, scancode, modifiers) -> onKeyboardPressed(currentScreen, key, scancode, modifiers)')
    return source


def main() -> None:
    for group in GROUPS:
        changed = 0
        for base in (ROOT / group / 'common' / 'src', ROOT / group / 'fabric' / 'src'):
            for file in base.rglob('*.java'):
                previous = file.read_text(encoding='utf-8')
                updated = migrate(previous, file.name)
                if updated != previous:
                    file.write_text(updated, encoding='utf-8', newline='')
                    changed += 1
        print(f'{group}: adapted {changed} files')


if __name__ == '__main__':
    main()
