package dev.petshopsoftware.utilities.Logging;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class Logger {
	public static final Map<String, Logger> LOGGERS = new HashMap<>();
	public static final LinkedList<Log> LOG_HISTORY = new LinkedList<>();
	private static final List<LogHandler> GLOBAL_HANDLERS = new ArrayList<>();
	public static String LOGS_DIRECTORY = "logs";
	public static String DEFAULT_FORMAT = "[%time%] [%level%] [%logger%] %message%";
	private static BufferedWriter WRITER = null;

	static {
		File logsDirectory = new File(LOGS_DIRECTORY);
		if (!logsDirectory.exists())
			if (!logsDirectory.mkdirs())
				try {
					throw new IOException("Logs directory could not be created.");
				} catch (IOException e) {
					Logger.get("main").error(Log.fromException(e));
				}

		String logFileName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date(System.currentTimeMillis())) + ".log";
		File file = new File(Paths.get(LOGS_DIRECTORY, logFileName).toUri());
		if (!file.exists())
			try {
				if (!file.createNewFile()) throw new IOException("Log file could not be created.");
				WRITER = new BufferedWriter(new FileWriter(file));
			} catch (IOException e) {
				Logger.get("main").error(Log.fromException(e));
			}
	}

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

	public static List<LogHandler> getGlobalHandlers() {
		return GLOBAL_HANDLERS;
	}

	public static void globalHandlers(LogHandler... handlers) {
		GLOBAL_HANDLERS.addAll(List.of(handlers));
	}

	synchronized public void log(Log message) {
		for (LogHandler handler : GLOBAL_HANDLERS)
			message = handler.preLog(message);
		for (LogHandler handler : handlers)
			message = handler.preLog(message);

		LOG_HISTORY.add(message);
		logs.add(message);

		if (WRITER != null) {
			try {
				WRITER.write(message.toString());
				WRITER.newLine();
				WRITER.flush();
			} catch (IOException e) {
				this.error(Log.fromException(new RuntimeException("Could not write to log file.")));
			}
		}

		PrintStream printStream;
		if (message.getLevel() == Level.ERROR || message.getLevel() == Level.FATAL)
			printStream = System.err;
		else printStream = System.out;
		if (message.getLevel().includes(this.level))
			printStream.println(message.colored());

		for (LogHandler handler : GLOBAL_HANDLERS)
			handler.postLog(message);
		for (LogHandler handler : handlers)
			handler.postLog(message);
	}

	public void log(Level level, String message, String format) {
		log(message(level, message, format));
	}

	public void log(Level level, String message) {
		log(level, message, format);
	}

	public Log message(Level level, String message, String format) {
		return new Log(id, level, message, System.currentTimeMillis(), format);
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

	public Logger handler(LogHandler handler) {
		handlers.add(handler);
		return this;
	}
}
