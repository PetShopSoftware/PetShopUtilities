package dev.petshopsoftware.utilities.Util;

import java.util.Random;
import java.util.UUID;

public class RandomUtil {
	public static final Random R = new Random();

	public static String generateIdentifier(int length) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		return uuid.substring(0, Math.min(length, uuid.length()));
	}
}
