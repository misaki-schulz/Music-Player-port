package info.u_team.music_player.musicplayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;

final class YoutubeTokenStore {

	private final Path path;

	YoutubeTokenStore(Path directory) {
		path = directory.resolve("youtube-oauth-token.dat");
	}

	String load() throws IOException {
		if (!Files.exists(path)) {
			return null;
		}
		final String token = Files.readString(path, StandardCharsets.UTF_8).trim();
		return token.isBlank() ? null : token;
	}

	void save(String token) throws IOException {
		final Path temporary = Files.createTempFile(path.getParent(), "youtube-oauth-", ".tmp");
		try {
			restrictToOwner(temporary);
			Files.writeString(temporary, token, StandardCharsets.UTF_8);
			try {
				Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
				Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static void restrictToOwner(Path file) throws IOException {
		final AclFileAttributeView acl = Files.getFileAttributeView(file, AclFileAttributeView.class);
		if (acl != null) {
			acl.setAcl(List.of(AclEntry.newBuilder()
					.setType(AclEntryType.ALLOW)
					.setPrincipal(Files.getOwner(file))
					.setPermissions(EnumSet.allOf(AclEntryPermission.class))
					.build()));
			return;
		}
		if (Files.getFileAttributeView(file, java.nio.file.attribute.PosixFileAttributeView.class) != null) {
			Files.setPosixFilePermissions(file, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
		}
	}
}
