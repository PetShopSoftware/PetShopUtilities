package dev.petshopsoftware.utilities.Util;

import java.util.Random;
import java.util.UUID;

public class RandomUtil {
	public static final Random R = new Random();

	public static String generateIdentifier(int length) {
		StringBuilder uuid = new StringBuilder();
		while (uuid.length() < length)
			uuid.append(UUID.randomUUID().toString().replace("-", ""));
		return uuid.substring(0, length);
	}

	public static String generateIdentifier() {
		return generateIdentifier(32);
	}
}
