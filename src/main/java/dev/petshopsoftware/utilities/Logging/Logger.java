package dev.petshopsoftware.utilities.Logging;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {
	public static final Map<String, Logger> LOGGERS = new ConcurrentHashMap<>();
	private static final ConcurrentLinkedDeque<LogMessage> GLOBAL_HISTORY = new ConcurrentLinkedDeque<>();
	private static final ConcurrentLinkedDeque<LogHandler> GLOBAL_HANDLERS = new ConcurrentLinkedDeque<>();
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
	private static String DEFAULT_FORMAT = "[%time%] [%level%] [%logger%] %message%";
	private static int HISTORY_LENGTH = 1000;
	private static String LOGS_DIRECTORY = "logs";
	private static BufferedWriter WRITER = null;

	static {
		StreamRedirect.redirect();
		setupOutputFile();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (WRITER != null) {
				try {
					WRITER.close();
				} catch (IOException e) {
					throw new RuntimeException("Could not close log file writer.", e);
				}
			}
			EXECUTOR_SERVICE.close();
		}));
	}

	private final String id;
	private final ConcurrentLinkedDeque<LogMessage> history = new ConcurrentLinkedDeque<>();
	private final ConcurrentLinkedDeque<LogHandler> handlers = new ConcurrentLinkedDeque<>();
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

	public static ConcurrentLinkedDeque<LogHandler> getGlobalHandlers() {
		return GLOBAL_HANDLERS;
	}

	public static void globalHandlers(LogHandler... handlers) {
		GLOBAL_HANDLERS.addAll(List.of(handlers));
	}

	public static String getLogsDirectory() {
		return LOGS_DIRECTORY;
	}

	public static void setLogsDirectory(String logsDirectory) {
		LOGS_DIRECTORY = logsDirectory;
		EXECUTOR_SERVICE.submit(Logger::setupOutputFile);
	}

	public static ConcurrentLinkedDeque<LogMessage> getGlobalHistory() {
		return GLOBAL_HISTORY;
	}

	public static int getHistoryLength() {
		return HISTORY_LENGTH;
	}

	public static void setHistoryLength(int historyLength) {
		HISTORY_LENGTH = historyLength;
	}

	public static String getDefaultFormat() {
		return DEFAULT_FORMAT;
	}

	public static void setDefaultFormat(String defaultFormat) {
		DEFAULT_FORMAT = defaultFormat;
	}

	protected static void setupOutputFile() {
		if (WRITER != null) {
			try {
				WRITER.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			WRITER = null;
		}
		if (LOGS_DIRECTORY == null) return;
		File logsDirectory = new File(LOGS_DIRECTORY);
		if (!logsDirectory.exists())
			if (!logsDirectory.mkdirs())
				try {
					throw new IOException("Logs directory could not be created.");
				} catch (IOException e) {
					Logger.get("main").error(LogMessage.fromException(e));
				}

		String logFileName = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date(System.currentTimeMillis())) + ".log";
		File file = new File(Paths.get(LOGS_DIRECTORY, logFileName).toUri());
		if (!file.exists())
			try {
				if (!file.createNewFile()) throw new IOException("Log file could not be created.");
				WRITER = new BufferedWriter(new FileWriter(file));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	}

	public void log(final LogMessage logMessage) {
		LogMessage message = logMessage;
		for (LogHandler handler : GLOBAL_HANDLERS)
			message = handler.preLog(message);
		for (LogHandler handler : handlers)
			message = handler.preLog(message);

		if (GLOBAL_HISTORY.size() < HISTORY_LENGTH) GLOBAL_HISTORY.add(message);
		if (history.size() < HISTORY_LENGTH) history.add(message);

		if (message.getLevel().includes(this.level)) {
			PrintStream printStream;
			if (message.getLevel() == Level.ERROR || message.getLevel() == Level.FATAL)
				printStream = System.err;
			else printStream = System.out;
			if (printStream instanceof StreamRedirect.RedirectedPrintStream redirectedPrintStream)
				redirectedPrintStream.printMessage(message);
			else printStream.println(message.colored());
		}

		for (LogHandler handler : GLOBAL_HANDLERS)
			handler.postLog(message);
		for (LogHandler handler : handlers)
			handler.postLog(message);

		LogMessage finalMessage = message;
		EXECUTOR_SERVICE.submit(() -> {
			if (WRITER != null) {
				try {
					WRITER.write(finalMessage.toString());
					WRITER.newLine();
					WRITER.flush();
				} catch (IOException e) {
					this.error(LogMessage.fromException(new RuntimeException("Could not write to log file.")));
				}
			}
		});
	}

	public void log(Level level, String message, String format) {
		log(message(level, message, format));
	}

	public void log(Level level, String message) {
		log(level, message, format);
	}

	public void log(Level level, Throwable throwable, String format) {
		log(level, LogMessage.fromException(throwable), format);
	}

	public void log(Level level, Throwable throwable) {
		log(level, LogMessage.fromException(throwable));
	}

	public LogMessage message(Level level, String message, String format) {
		return new LogMessage(id, level, message, System.currentTimeMillis(), format);
	}

	public LogMessage message(Level level, String message) {
		return new LogMessage(id, level, message, System.currentTimeMillis(), format);
	}

	public LogMessage message(Level level, Throwable throwable, String format) {
		return message(level, LogMessage.fromException(throwable), format);
	}

	public LogMessage message(Level level, Throwable throwable) {
		return message(level, LogMessage.fromException(throwable), format);
	}

	public void fatal(String message) {
		log(Level.FATAL, message);
	}

	public void fatal(Throwable throwable) {
		fatal(LogMessage.fromException(throwable));
	}

	public void error(String message) {
		log(Level.ERROR, message);
	}

	public void error(Throwable throwable) {
		error(LogMessage.fromException(throwable));
	}

	public void warn(String message) {
		log(Level.WARN, message);
	}

	public void warn(Throwable throwable) {
		warn(LogMessage.fromException(throwable));
	}

	public void info(String message) {
		log(Level.INFO, message);
	}

	public void info(Throwable throwable) {
		info(LogMessage.fromException(throwable));
	}

	public void debug(String message) {
		log(Level.DEBUG, message);
	}

	public void debug(Throwable throwable) {
		debug(LogMessage.fromException(throwable));
	}

	public String getID() {
		return id;
	}

	public ConcurrentLinkedDeque<LogMessage> getHistory() {
		return history;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public ConcurrentLinkedDeque<LogHandler> getHandlers() {
		return handlers;
	}

	public Logger handlers(LogHandler... handlers) {
		this.handlers.addAll(List.of(handlers));
		return this;
	}
}
