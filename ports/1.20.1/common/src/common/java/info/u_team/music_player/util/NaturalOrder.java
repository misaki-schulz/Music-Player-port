package info.u_team.music_player.util;

/** Case-insensitive ordering with numeric runs: Track 2 precedes Track 10. */
public final class NaturalOrder {

	private NaturalOrder() {
	}

	public static int compare(String left, String right) {
		left = left == null ? "" : left;
		right = right == null ? "" : right;
		int a = 0;
		int b = 0;
		while (a < left.length() && b < right.length()) {
			final char x = left.charAt(a);
			final char y = right.charAt(b);
			if (isDigit(x) && isDigit(y)) {
				int endA = a;
				int endB = b;
				while (endA < left.length() && isDigit(left.charAt(endA))) endA++;
				while (endB < right.length() && isDigit(right.charAt(endB))) endB++;
				while (a < endA && left.charAt(a) == '0') a++;
				while (b < endB && right.charAt(b) == '0') b++;
				final int length = Integer.compare(endA - a, endB - b);
				if (length != 0) return length;
				while (a < endA) {
					final int digit = Character.compare(left.charAt(a++), right.charAt(b++));
					if (digit != 0) return digit;
				}
				a = endA;
				b = endB;
			} else {
				final int character = Character.compare(Character.toLowerCase(x), Character.toLowerCase(y));
				if (character != 0) return character;
				a++;
				b++;
			}
		}
		return Integer.compare(left.length() - a, right.length() - b);
	}

	private static boolean isDigit(char value) {
		return value >= '0' && value <= '9';
	}
}
