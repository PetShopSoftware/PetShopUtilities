package dev.petshopsoftware.utilities.Logging;

import dev.petshopsoftware.utilities.JSON.JSON;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogMessage implements JSON, Cloneable {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	private final String logger;
	private final long timestamp;
	private Level level;
	private String message;
	private String format;

	public LogMessage(String logger, Level level, String message, long timestamp, String format) {
		this.logger = logger;
		this.level = level;
		this.message = message;
		this.timestamp = timestamp;
		this.format = format;
	}

	public static String fromException(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		pw.close();
		String string = sw.toString();
		return string;
	}

	@Override
	public String toString() {
		if (format == null) return message;
		return format
				.replace("%time%", DATE_FORMAT.format(new Date(timestamp)))
				.replace("%level%", level.name())
				.replace("%logger%", logger)
				.replace("%message%", message);
	}

	public String colored() {
		return level.getColor() + this + Level.RESET;
	}

	public String getLogger() {
		return logger;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Level getLevel() {
		return level;
	}

	public LogMessage level(Level level) {
		this.level = level;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public LogMessage message(String message) {
		this.message = message;
		return this;
	}

	public String getFormat() {
		return format;
	}

	public LogMessage format(String format) {
		this.format = format;
		return this;
	}

	public void log() {
		Logger.get(logger).log(this);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
