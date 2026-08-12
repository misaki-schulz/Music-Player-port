// Binary bridge for raw GUI texture rendering across Minecraft 1.21.2-1.21.11.
package info.u_team.music_player.gui.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class GuiTextureCompat {

	private static volatile Class<?> initializedGraphicsClass;
	private static volatile Method directBlit;
	private static volatile Method legacyBlit;
	private static volatile Method guiTexturedFactory;

	private GuiTextureCompat() {
	}

	public static void blit(GuiGraphics graphics, ResourceLocation image, int left, int top, int right, int bottom) {
		if (image == null || right <= left || bottom <= top) {
			return;
		}
		initialize(graphics.getClass(), image.getClass());
		try {
			if (directBlit != null) {
				directBlit.invoke(graphics, image, left, top, right, bottom, 0F, 1F, 0F, 1F);
				return;
			}

			final int width = right - left;
			final int height = bottom - top;
			final Function<ResourceLocation, RenderType> renderType = GuiTextureCompat::createGuiTexturedRenderType;
			legacyBlit.invoke(graphics, renderType, image, left, top, 0F, 0F, width, height, width, height);
		} catch (final IllegalAccessException ex) {
			throw new IllegalStateException("Cannot access the Minecraft GUI texture renderer", ex);
		} catch (final InvocationTargetException ex) {
			final Throwable cause = ex.getCause();
			if (cause instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Minecraft GUI texture rendering failed", cause);
		}
	}

	private static void initialize(Class<?> graphicsClass, Class<?> resourceClass) {
		if (graphicsClass == initializedGraphicsClass) {
			return;
		}
		synchronized (GuiTextureCompat.class) {
			if (graphicsClass == initializedGraphicsClass) {
				return;
			}
			directBlit = null;
			legacyBlit = null;
			for (final Method method : graphicsClass.getMethods()) {
				final Class<?>[] parameters = method.getParameterTypes();
				if (method.getReturnType() != void.class) {
					continue;
				}
				if (parameters.length == 9
						&& parameters[0].isAssignableFrom(resourceClass)
						&& are(parameters, 1, int.class, int.class, int.class, int.class,
								float.class, float.class, float.class, float.class)) {
					directBlit = method;
					break;
				}
				if (parameters.length == 10
						&& Function.class.isAssignableFrom(parameters[0])
						&& parameters[1].isAssignableFrom(resourceClass)
						&& are(parameters, 2, int.class, int.class, float.class, float.class,
								int.class, int.class, int.class, int.class)) {
					legacyBlit = method;
				}
			}
			if (directBlit == null && legacyBlit == null) {
				throw new IllegalStateException("Compatible GuiGraphics texture renderer not found in " + graphicsClass.getName());
			}
			initializedGraphicsClass = graphicsClass;
		}
	}

	private static boolean are(Class<?>[] actual, int offset, Class<?>... expected) {
		for (int index = 0; index < expected.length; index++) {
			if (actual[offset + index] != expected[index]) {
				return false;
			}
		}
		return true;
	}

	private static RenderType createGuiTexturedRenderType(ResourceLocation image) {
		try {
			Method factory = guiTexturedFactory;
			if (factory == null) {
				factory = findGuiTexturedFactory();
				guiTexturedFactory = factory;
			}
			return (RenderType) factory.invoke(null, image);
		} catch (final IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Cannot create the legacy GUI render type", ex);
		}
	}

	private static Method findGuiTexturedFactory() {
		for (final Method method : RenderType.class.getMethods()) {
			final String name = method.getName();
			if ((name.equals("guiTextured") || name.equals("method_62277"))
					&& Modifier.isStatic(method.getModifiers())
					&& method.getReturnType() == RenderType.class
					&& method.getParameterCount() == 1
					&& method.getParameterTypes()[0] == ResourceLocation.class) {
				return method;
			}
		}
		throw new IllegalStateException("Legacy RenderType.guiTextured factory not found");
	}
}
