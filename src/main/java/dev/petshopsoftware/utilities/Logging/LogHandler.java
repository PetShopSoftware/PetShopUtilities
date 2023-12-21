package dev.petshopsoftware.utilities.Logging;

public interface LogHandler {
	Log preLog(Log message);

	void postLog(Log message);
}
