// Modified for Minecraft 26.x by misaki-schulz; see NOTICE.
package info.u_team.music_player.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Bridges the screen API move between Minecraft 26.1 and 26.2.
 * <p>
 * In 26.1, {@code screen} and {@code setScreen} belong to {@link Minecraft}.
 * In 26.2, they belong to the object exposed by {@code Minecraft.gui}.
 */
public final class MinecraftGuiCompat {

	private static final Field DIRECT_SCREEN = findField(Minecraft.class, "screen");
	private static final Method DIRECT_SET_SCREEN = findMethod(Minecraft.class, "setScreen", Screen.class);
	private static final Field GUI = findField(Minecraft.class, "gui");
	private static final Method GUI_SCREEN = GUI == null ? null : findMethod(GUI.getType(), "screen");
	private static final Method GUI_SET_SCREEN = GUI == null ? null : findMethod(GUI.getType(), "setScreen", Screen.class);

	private MinecraftGuiCompat() {
	}

	public static Screen getScreen(Minecraft minecraft) {
		try {
			if (DIRECT_SCREEN != null) {
				return (Screen) DIRECT_SCREEN.get(minecraft);
			}
			if (GUI != null && GUI_SCREEN != null) {
				return (Screen) GUI_SCREEN.invoke(GUI.get(minecraft));
			}
		} catch (final IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Cannot read the current Minecraft screen", ex);
		}
		throw new IllegalStateException("Unsupported Minecraft screen API");
	}

	public static void setScreen(Minecraft minecraft, Screen screen) {
		try {
			if (DIRECT_SET_SCREEN != null) {
				DIRECT_SET_SCREEN.invoke(minecraft, screen);
				return;
			}
			if (GUI != null && GUI_SET_SCREEN != null) {
				GUI_SET_SCREEN.invoke(GUI.get(minecraft), screen);
				return;
			}
		} catch (final IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Cannot change the current Minecraft screen", ex);
		}
		throw new IllegalStateException("Unsupported Minecraft screen API");
	}

	private static Field findField(Class<?> owner, String name) {
		try {
			return owner.getField(name);
		} catch (final NoSuchFieldException ex) {
			return null;
		}
	}

	private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
		try {
			return owner.getMethod(name, parameterTypes);
		} catch (final NoSuchMethodException ex) {
			return null;
		}
	}
}
