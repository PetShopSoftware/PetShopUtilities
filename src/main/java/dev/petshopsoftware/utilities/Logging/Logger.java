package dev.petshopsoftware.utilities.Logging;

import java.io.PrintStream;
import java.util.*;

public class Logger {
	public static final Map<String, Logger> LOGGERS = new HashMap<>();
	public static final LinkedList<Log> LOG_HISTORY = new LinkedList<>();
	public static String LOGS_DIRECTORY = "./logs";
	public static String DEFAULT_FORMAT = "[%time%] [%level%] [%logger%] %message%";

	private final String id;
	private final LinkedList<Log> logs = new LinkedList<>();
	private final List<LogHandler> handlers = new ArrayList<>();
	private String format;
	private Level level;

	public Logger(String id, Level level, String format) {
		id = id.toLowerCase().replace(" ", "-");
		if (LOGGERS.containsKey(id))
			throw new IllegalArgumentException("Duplicate logger with name " + id + ".");
		this.id = id;
		this.format = format;
		this.level = level;
		LOGGERS.put(id, this);
	}

	public Logger(String id, Level level) {
		this(id, level, DEFAULT_FORMAT);
	}

	public Logger(String id) {
		this(id, Level.DEBUG, DEFAULT_FORMAT);
	}

	public static Logger get(String id) {
		id = id.toLowerCase().replace(" ", "-");
		Logger logger = LOGGERS.get(id);
		if (logger == null) return new Logger(id);
		return logger;
	}

	public Log message(Level level, String message, String format) {
		return new Log(id, level, message, System.currentTimeMillis(), format);
	}

	synchronized public void log(Log message) {
		for (LogHandler handler : handlers)
			message = handler.preLog(message);

		LOG_HISTORY.add(message);
		logs.add(message);

		PrintStream printStream;
		if (level == Level.ERROR || level == Level.FATAL)
			printStream = System.err;
		else printStream = System.out;
		printStream.println(message.colored());

		for (LogHandler handler : handlers)
			handler.postLog(message);
	}

	public void log(Level level, String message, String format) {
		log(message(level, message, format));
	}

	public void log(Level level, String message) {
		log(level, message, format);
	}

	public void fatal(String message) {
		log(Level.FATAL, message);
	}

	public void error(String message) {
		log(Level.ERROR, message);
	}

	public void warn(String message) {
		log(Level.WARN, message);
	}

	public void info(String message) {
		log(Level.INFO, message);
	}

	public void debug(String message) {
		log(Level.DEBUG, message);
	}

	public String getID() {
		return id;
	}

	public LinkedList<Log> getLogs() {
		return logs;
	}

	public String getFormat() {
		return format;
	}

	public void format(String format) {
		this.format = format;
	}

	public Level getLevel() {
		return level;
	}

	public void level(Level level) {
		this.level = level;
	}

	public List<LogHandler> getHandlers() {
		return handlers;
	}

	public void handler(LogHandler handler) {
		handlers.add(handler);
	}
}
