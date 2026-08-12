// Binary bridge for the PoseStack to Matrix3x2fStack transition in Minecraft 1.21.x.
package info.u_team.music_player.gui.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class LegacyGuiTransform {

	private static volatile Method poseAccessor;

	private LegacyGuiTransform() {
	}

	public static void transformed(GuiGraphics graphics, float x, float y, float scale, Runnable draw) {
		final Object pose = pose(graphics);
		if (pose instanceof PoseStack stack) {
			stack.pushPose();
			stack.translate(x, y, 500);
			stack.scale(scale, scale, 1);
			try {
				draw.run();
			} finally {
				stack.popPose();
			}
			return;
		}
		if (pose instanceof Matrix3x2fStack stack) {
			stack.pushMatrix();
			stack.translate(x, y);
			stack.scale(scale, scale);
			try {
				draw.run();
			} finally {
				stack.popMatrix();
			}
			return;
		}
		throw new IllegalStateException("Unsupported Minecraft GUI pose stack " + pose.getClass().getName());
	}

	public static int[] transformRect(GuiGraphics graphics, float x1, float y1, float x2, float y2) {
		final Object pose = pose(graphics);
		if (pose instanceof PoseStack stack) {
			final Matrix4f matrix = stack.last().pose();
			final Vector4f from = new Vector4f(x1, y1, 0, 1).mul(matrix);
			final Vector4f to = new Vector4f(x2, y2, 0, 1).mul(matrix);
			return new int[] { Mth.ceil(from.x), Mth.ceil(from.y), Mth.ceil(to.x), Mth.ceil(to.y) };
		}
		if (pose instanceof Matrix3x2fStack stack) {
			final float fromX = stack.m00() * x1 + stack.m10() * y1 + stack.m20();
			final float fromY = stack.m01() * x1 + stack.m11() * y1 + stack.m21();
			final float toX = stack.m00() * x2 + stack.m10() * y2 + stack.m20();
			final float toY = stack.m01() * x2 + stack.m11() * y2 + stack.m21();
			return new int[] { Mth.ceil(fromX), Mth.ceil(fromY), Mth.ceil(toX), Mth.ceil(toY) };
		}
		throw new IllegalStateException("Unsupported Minecraft GUI pose stack " + pose.getClass().getName());
	}

	private static Object pose(GuiGraphics graphics) {
		try {
			Method accessor = poseAccessor;
			if (accessor == null || !accessor.getDeclaringClass().isAssignableFrom(graphics.getClass())) {
				accessor = findPoseAccessor(graphics.getClass());
				poseAccessor = accessor;
			}
			return accessor.invoke(graphics);
		} catch (final IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Cannot access the Minecraft GUI pose stack", ex);
		}
	}

	private static Method findPoseAccessor(Class<?> graphicsClass) {
		for (final Method method : graphicsClass.getMethods()) {
			if (method.getParameterCount() == 0
					&& (PoseStack.class.isAssignableFrom(method.getReturnType())
							|| Matrix3x2fStack.class.isAssignableFrom(method.getReturnType()))) {
				return method;
			}
		}
		throw new IllegalStateException("Compatible GUI pose accessor not found in " + graphicsClass.getName());
	}
}
