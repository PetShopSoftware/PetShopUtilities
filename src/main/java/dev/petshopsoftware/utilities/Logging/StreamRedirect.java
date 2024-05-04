package dev.petshopsoftware.utilities.Logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class StreamRedirect {
	public static void redirectOut() {
		PrintStream out = new RedirectedPrintStream(System.out, Logger.get("main"), Level.INFO);
		System.setOut(out);
	}

	public static void redirectErr() {
		PrintStream err = new RedirectedPrintStream(System.err, Logger.get("main"), Level.ERROR);
		System.setErr(err);
	}

	public static void redirect() {
		redirectOut();
		redirectErr();
	}

	static class RedirectedPrintStream extends PrintStream {
		public final ThreadLocal<Boolean> halted = ThreadLocal.withInitial(() -> false);
		public final Logger logger;
		public final Level level;

		public RedirectedPrintStream(OutputStream out, Logger logger, Level level) {
			super(out);
			this.logger = logger;
			this.level = level;
		}

		@Override
		public void println(int x) {
			logger.log(level, String.valueOf(x));
		}

		@Override
		public void println(char x) {
			logger.log(level, String.valueOf(x));
		}

		@Override
		public void println(boolean x) {
			logger.log(level, String.valueOf(x));
		}

		@Override
		public void println(long x) {
			logger.log(level, String.valueOf(x));
		}

		@Override
		public void println(float x) {
			logger.log(level, String.valueOf(x));
		}

		@Override
		public void println(double x) {
			logger.log(level, String.valueOf(x));
		}

		@Override
		public void println(char[] x) {
			logger.log(level, Arrays.toString(x));
		}

		@Override
		public void println(String x) {
			logger.log(level, x);
		}

		@Override
		public void println(Object x) {
			logger.log(level, x == null ? "null" : x.toString());
		}

		public void printMessage(LogMessage message) {
			super.println(message.colored());
		}
	}
}
