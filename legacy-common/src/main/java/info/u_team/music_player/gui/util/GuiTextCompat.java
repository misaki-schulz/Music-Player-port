// Binary bridge for the drawString return-type change inside Minecraft 1.21.x.
package info.u_team.music_player.gui.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class GuiTextCompat {

	private static final Map<Class<?>, Method> DRAW_STRING = new ConcurrentHashMap<>();

	private GuiTextCompat() {
	}

	public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
		draw(graphics, font, text, x, y, color, false);
	}

	public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
		try {
			DRAW_STRING.computeIfAbsent(graphics.getClass(), GuiTextCompat::findDrawString)
					.invoke(graphics, font, text == null ? "" : text, x, y, normalizeColor(color), shadow);
		} catch (final ReflectiveOperationException ex) {
			throw new IllegalStateException("Cannot invoke the Minecraft GUI text renderer", ex);
		}
	}

	private static int normalizeColor(int color) {
		// Older Music Player sources use RGB literals. Newer Minecraft versions
		// interpret their missing high byte as alpha=0 and render the text invisible.
		return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
	}

	private static Method findDrawString(Class<?> graphicsClass) {
		for (final Method method : graphicsClass.getMethods()) {
			final Class<?>[] parameters = method.getParameterTypes();
			if (parameters.length == 6
					&& Font.class.isAssignableFrom(parameters[0])
					&& parameters[1] == String.class
					&& parameters[2] == int.class
					&& parameters[3] == int.class
					&& parameters[4] == int.class
					&& parameters[5] == boolean.class) {
				return method;
			}
		}
		throw new IllegalStateException("Compatible GuiGraphics.drawString method not found in " + graphicsClass.getName());
	}
}
