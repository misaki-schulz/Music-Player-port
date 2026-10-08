package info.u_team.music_player.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import info.u_team.music_player.MusicPlayerReference;

public final class LoaderPlatform {
    private LoaderPlatform() {}

    public static Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }

    // FML 10+ exposes JarContents instead of a mounted resource filesystem.
    public static Path modResource(String folder) {
        try {
            final Path directory = Files.createTempDirectory("musicplayer-bundled-");
            directory.toFile().deleteOnExit();
            final var contents = ModList.get().getModFileById(MusicPlayerReference.MODID).getFile().getContents();
            contents.visitContent(folder + "/", (name, resource) -> {
                if (!name.endsWith(".jar.packed")) return;
                final Path destination = directory.resolve(Path.of(name).getFileName().toString());
                try (final InputStream stream = resource.open()) {
                    Files.copy(stream, destination);
                    destination.toFile().deleteOnExit();
                } catch (final IOException exception) {
                    throw new UncheckedIOException("Cannot read bundled audio library " + name, exception);
                }
            });
            try (final var files = Files.list(directory)) {
                if (files.findAny().isEmpty()) throw new IllegalStateException("No bundled audio libraries in " + folder);
            }
            return directory;
        } catch (final IOException exception) {
            throw new UncheckedIOException("Cannot extract loader resources", exception);
        }
    }
}
