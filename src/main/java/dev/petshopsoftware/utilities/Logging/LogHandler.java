package dev.petshopsoftware.utilities.Logging;

public interface LogHandler {
	LogMessage preLog(LogMessage message);

	void postLog(LogMessage message);
}
