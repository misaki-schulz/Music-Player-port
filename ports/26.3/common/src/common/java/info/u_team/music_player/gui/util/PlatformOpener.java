package info.u_team.music_player.gui.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

/** Opens a browser or local file without relying on removed Minecraft OS methods. */
public final class PlatformOpener {
	private PlatformOpener() {
	}

	public static void openUri(URI uri) {
		open(Desktop.Action.BROWSE, desktop -> desktop.browse(uri));
	}

	public static void openFile(File file) {
		open(Desktop.Action.OPEN, desktop -> desktop.open(file));
	}

	private static void open(Desktop.Action action, DesktopOperation operation) {
		if (!Desktop.isDesktopSupported()) {
			throw new UnsupportedOperationException("Desktop integration is unavailable");
		}
		final Desktop desktop = Desktop.getDesktop();
		if (!desktop.isSupported(action)) {
			throw new UnsupportedOperationException("Desktop action " + action + " is unavailable");
		}
		try {
			operation.run(desktop);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	@FunctionalInterface
	private interface DesktopOperation {
		void run(Desktop desktop) throws IOException;
	}
}
