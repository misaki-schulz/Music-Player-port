// Shared reflective bridge for the screen API move between supported Minecraft versions; see NOTICE.
package info.u_team.music_player.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MinecraftGuiCompat {

	private static final Field DIRECT_SCREEN = findField(Minecraft.class, "screen", Screen.class);
	private static final Method DIRECT_SET_SCREEN = findScreenSetter(Minecraft.class, "setScreen");
	private static final Field GUI = findField(Minecraft.class, "gui");
	private static final Method GUI_SCREEN = GUI == null ? null : findScreenGetter(GUI.getType(), "screen");
	private static final Method GUI_SET_SCREEN = GUI == null ? null : findScreenSetter(GUI.getType(), "setScreen");

	private MinecraftGuiCompat() {
	}

	public static Screen getScreen(Minecraft minecraft) {
		try {
			if (DIRECT_SCREEN != null) return (Screen) DIRECT_SCREEN.get(minecraft);
			if (GUI != null && GUI_SCREEN != null) return (Screen) GUI_SCREEN.invoke(GUI.get(minecraft));
		} catch (final IllegalAccessException | InvocationTargetException exception) {
			throw new IllegalStateException("Cannot read the current Minecraft screen", exception);
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
		} catch (final IllegalAccessException | InvocationTargetException exception) {
			throw new IllegalStateException("Cannot change the current Minecraft screen", exception);
		}
		throw new IllegalStateException("Unsupported Minecraft screen API");
	}

	public static void showActionBar(Minecraft minecraft, Component message) {
		if (minecraft == null || message == null || minecraft.player == null) return;
		if (invokeMessage(minecraft.player, message, true,
				"displayClientMessage", "sendSystemMessage")) return;
		try {
			final Field guiField = findField(Minecraft.class, "gui");
			if (guiField != null && invokeMessage(guiField.get(minecraft), message, true,
					"setOverlayMessage", "displayClientMessage")) return;
		} catch (final IllegalAccessException ignored) {
			// Fall through to the one-argument chat-message fallback below.
		}
		invokeMessage(minecraft.player, message, false, "sendSystemMessage");
	}

	private static boolean invokeMessage(Object target, Component message, boolean withOverlayFlag, String... names) {
		if (target == null) return false;
		for (final String name : names) {
			for (final Method method : target.getClass().getMethods()) {
				final Class<?>[] parameters = method.getParameterTypes();
				if (!method.getName().equals(name) || parameters.length != (withOverlayFlag ? 2 : 1)
						|| !parameters[0].isAssignableFrom(message.getClass())
						|| (withOverlayFlag && parameters[1] != boolean.class)) continue;
				try {
					if (withOverlayFlag) method.invoke(target, message, true);
					else method.invoke(target, message);
					return true;
				} catch (final IllegalAccessException | InvocationTargetException ignored) {
					return false;
				}
			}
		}
		return false;
	}

	private static Field findField(Class<?> owner, String name) {
		try {
			return owner.getField(name);
		} catch (final NoSuchFieldException exception) {
			return null;
		}
	}

	private static Field findField(Class<?> owner, String name, Class<?> fieldType) {
		final Field named = findField(owner, name);
		if (named != null && fieldType.isAssignableFrom(named.getType())) return named;
		for (final Field field : owner.getFields()) {
			if (fieldType.isAssignableFrom(field.getType())) return field;
		}
		return null;
	}

	private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
		try {
			return owner.getMethod(name, parameterTypes);
		} catch (final NoSuchMethodException exception) {
			return null;
		}
	}

	private static Method findScreenSetter(Class<?> owner, String name) {
		final Method named = findMethod(owner, name, Screen.class);
		if (named != null) return named;
		for (final Method method : owner.getMethods()) {
			final Class<?>[] parameters = method.getParameterTypes();
			if (parameters.length == 1 && parameters[0] == Screen.class && method.getReturnType() == void.class) return method;
		}
		return null;
	}

	private static Method findScreenGetter(Class<?> owner, String name) {
		final Method named = findMethod(owner, name);
		if (named != null && Screen.class.isAssignableFrom(named.getReturnType())) return named;
		for (final Method method : owner.getMethods()) {
			if (method.getParameterCount() == 0 && Screen.class.isAssignableFrom(method.getReturnType())) return method;
		}
		return null;
	}
}
