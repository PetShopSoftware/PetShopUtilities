package dev.petshopsoftware.utilities.HTTP.Server;

import java.lang.reflect.Method;

public interface HTTPHandler {
	boolean matchesRoute(Route route, Method method);

	HTTPResponse handle(Route route, HTTPData data);
}
