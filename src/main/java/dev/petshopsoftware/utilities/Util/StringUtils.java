package dev.petshopsoftware.utilities.Util;

public class StringUtils {
	public static String padLeft(String str, int pad, char padChar) {
		String padding = new String(new char[pad]).replace('\0', padChar);
		String[] lines = str.split("\n");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			builder.append(padding).append(lines[i]);
			if (i < lines.length - 1) builder.append("\n");
		}
		return builder.toString();
	}

	public static String padLeft(String str, int pad) {
		return padLeft(str, pad, ' ');
	}

	public static String padRight(String str, int pad, char padChar) {
		String padding = new String(new char[pad]).replace('\0', padChar);
		String[] lines = str.split("\n");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			builder.append(lines[i]).append(padding);
			if (i < lines.length - 1) builder.append("\n");
		}
		return builder.toString();
	}

	public static String padRight(String str, int pad) {
		return padLeft(str, pad, ' ');
	}
}
