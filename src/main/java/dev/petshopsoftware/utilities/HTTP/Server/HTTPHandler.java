package dev.petshopsoftware.utilities.HTTP.Server;

public interface HTTPHandler {
	boolean matchesRoute(Route route);

	HTTPResponse handle(Route route, HTTPData data);
}
