// Binary bridge for the Screen init/resize change between Minecraft 1.21.10 and 1.21.11; see NOTICE.
package info.u_team.music_player.gui.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class LegacyScreenCompat {

	private static final Map<Class<?>, Method> INIT_METHODS = new ConcurrentHashMap<>();

	private LegacyScreenCompat() {
	}

	public static void reinitialize(Screen screen, int width, int height) {
		try {
			final Method method = INIT_METHODS.computeIfAbsent(screen.getClass(), LegacyScreenCompat::findInitMethod);
			if (method.getParameterCount() == 3) {
				method.invoke(screen, Minecraft.getInstance(), width, height);
			} else {
				method.invoke(screen, width, height);
			}
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Unable to reinitialize Minecraft screen", exception);
		}
	}

	private static Method findInitMethod(Class<?> screenClass) {
		for (final Method method : screenClass.getMethods()) {
			if (!Modifier.isFinal(method.getModifiers()) || method.getReturnType() != void.class) {
				continue;
			}
			final Class<?>[] parameters = method.getParameterTypes();
			if (parameters.length == 3 && parameters[0] == Minecraft.class && parameters[1] == int.class && parameters[2] == int.class) {
				return method;
			}
			if (parameters.length == 2 && parameters[0] == int.class && parameters[1] == int.class) {
				return method;
			}
		}
		throw new IllegalStateException("Unable to find the final Screen initialization method on " + screenClass.getName());
	}
}
