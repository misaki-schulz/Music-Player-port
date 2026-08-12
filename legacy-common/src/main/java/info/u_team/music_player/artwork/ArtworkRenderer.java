package info.u_team.music_player.artwork;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;

import info.u_team.music_player.gui.util.GuiTextureCompat;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public final class ArtworkRenderer {

	private static final int MAX_TEXTURES = 128;
	private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
	private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();

	private ArtworkRenderer() {
	}

	public static void render(GuiGraphics graphics, IAudioTrack track, int x, int y, int size) {
		final String url = ArtworkRepository.resolveURL(track.getInfo());
		if (url == null) {
			placeholder(graphics, x, y, size);
			return;
		}
		final String key = ArtworkRepository.cacheKey(url);
		final ResourceLocation texture = TEXTURES.get(key);
		if (texture != null) {
			GuiTextureCompat.blit(graphics, texture, x, y, x + size, y + size);
			return;
		}
		placeholder(graphics, x, y, size);
		if (LOADING.putIfAbsent(key, Boolean.TRUE) == null) {
			ArtworkRepository.request(track.getInfo()).whenComplete((bytes, error) -> {
				if (error != null) {
					LOADING.remove(key);
					return;
				}
				Minecraft.getInstance().execute(() -> register(key, bytes));
			});
		}
	}

	private static void register(String key, byte[] bytes) {
		try {
			final NativeImage image = NativeImage.read(bytes);
			if (image.getWidth() <= 0 || image.getHeight() <= 0 || image.getWidth() > 4096 || image.getHeight() > 4096) {
				image.close();
				return;
			}
			while (TEXTURES.size() >= MAX_TEXTURES) {
				final String oldest = TEXTURES.keySet().stream().findFirst().orElse(null);
				if (oldest == null) break;
				final ResourceLocation removed = TEXTURES.remove(oldest);
				if (removed != null) Minecraft.getInstance().getTextureManager().release(removed);
			}
			final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("musicplayer", "artwork/" + key);
			Minecraft.getInstance().getTextureManager().register(id, createTexture(image));
			TEXTURES.put(key, id);
		} catch (final IOException | ReflectiveOperationException ignored) {
		} finally {
			LOADING.remove(key);
		}
	}

	private static AbstractTexture createTexture(NativeImage image) throws ReflectiveOperationException {
		try {
			return (AbstractTexture) DynamicTexture.class.getConstructor(NativeImage.class).newInstance(image);
		} catch (final NoSuchMethodException legacyConstructorMissing) {
			return (AbstractTexture) DynamicTexture.class.getConstructor(Supplier.class, NativeImage.class)
					.newInstance((Supplier<String>) () -> "Music Player artwork", image);
		}
	}

	private static void placeholder(GuiGraphics graphics, int x, int y, int size) {
		graphics.fill(x, y, x + size, y + size, 0xE0181C21);
		graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF292F36);
		final int center = size / 2;
		graphics.fill(x + center - 1, y + size / 4, x + center + 2, y + size * 3 / 4, 0xFF7E8994);
		graphics.fill(x + center + 1, y + size / 4, x + size * 3 / 4, y + size / 4 + 2, 0xFF7E8994);
		graphics.fill(x + size / 4, y + size * 3 / 4 - 2, x + center + 2, y + size * 3 / 4 + 2, 0xFFB8C2CC);
	}
}
