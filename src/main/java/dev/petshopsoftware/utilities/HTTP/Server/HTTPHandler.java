package dev.petshopsoftware.utilities.HTTP.Server;

import java.lang.reflect.Method;

public interface HTTPHandler {
	boolean matchesRoute(HTTPData data, Route route, Method method);

	HTTPResponse handle(HTTPData data, Method method);
}
