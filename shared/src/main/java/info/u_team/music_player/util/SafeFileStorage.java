package info.u_team.music_player.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Atomic file replacement with a small rotating backup set. */
public final class SafeFileStorage {

	private static final int BACKUP_COUNT = 3;

	private SafeFileStorage() {
	}

	public static List<Path> readCandidates(Path target) {
		final List<Path> candidates = new ArrayList<>();
		candidates.add(target);
		for (int index = 1; index <= BACKUP_COUNT; index++) {
			candidates.add(backupPath(target, index));
		}
		return candidates;
	}

	public static synchronized void writeAtomically(Path target, OutputWriter writer) throws IOException {
		Files.createDirectories(target.toAbsolutePath().getParent());
		final Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
		try {
			try (OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				writer.write(output);
			}
			try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
				channel.force(true);
			}
			rotateBackups(target);
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (final AtomicMoveNotSupportedException exception) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static void rotateBackups(Path target) throws IOException {
		for (int index = BACKUP_COUNT; index >= 2; index--) {
			final Path previous = backupPath(target, index - 1);
			if (Files.exists(previous)) {
				Files.move(previous, backupPath(target, index), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		if (Files.exists(target)) {
			Files.copy(target, backupPath(target, 1), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		}
	}

	private static Path backupPath(Path target, int index) {
		return target.resolveSibling(target.getFileName() + ".bak." + index);
	}

	@FunctionalInterface
	public interface OutputWriter {
		void write(OutputStream output) throws IOException;
	}
}
