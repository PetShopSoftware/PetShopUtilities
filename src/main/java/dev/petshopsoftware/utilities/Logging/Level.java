package dev.petshopsoftware.utilities.Logging;

public enum Level {
	FATAL("\u001B[41m\u001B[30m"),
	ERROR("\u001B[31m"),
	WARN("\u001B[33m"),
	INFO("\033[0;32m"),
	DEBUG("\033[38;5;51m"),
	;

	public static final String RESET = "\033[0m";
	private final String color;

	Level(String color) {
		this.color = color;
	}

	public String getColor() {
		return color;
	}

	public boolean includes(Level level) {
		return level.ordinal() >= this.ordinal();
	}
}
