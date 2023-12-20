package dev.petshopsoftware.utilities.HTTP.Server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.petshopsoftware.utilities.JSON.ObjectBuilder;
import dev.petshopsoftware.utilities.Util.ParsingMode;
import dev.petshopsoftware.utilities.Util.ReflectionUtil;

import javax.naming.NameNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPServer {
	private final HttpServer server;
	private final Map<String, Method> routes = new HashMap<>();

	public HTTPServer(int port) {
		try {
			this.server = HttpServer.create(new InetSocketAddress(port), 0);
			init();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void init() {
		server.createContext("/", this::handleRequest);
	}

	protected void handleRequest(HttpExchange exchange) {
		HTTPMethod method = HTTPMethod.valueOf(exchange.getRequestMethod());
		String path = exchange.getRequestURI().toString();
		HTTPResponse response = null;
		Exception exception = null;
		try {
			response = resolveRoute(exchange, method, path);
		} catch (NameNotFoundException e) {
			response = getNotFound();
			exception = e;
		} catch (InvocationTargetException e) {
			response = getInternalError();
			exception = e;
		}
		handleRequestResponse(exchange, response, exception);
		send(exchange, response);
	}

	protected HTTPResponse resolveRoute(HttpExchange exchange, HTTPMethod method, String path) throws NameNotFoundException, InvocationTargetException {
		String[] pathSegments = path.split("/");
		for (String routeID : routes.keySet()) {
			String[] routeIDParts = routeID.split(" ");
			HTTPMethod routeMethod = HTTPMethod.valueOf(routeIDParts[0]);
			if (method != routeMethod) continue;
			String[] routeSegments = routeIDParts[1].split("/");
			if (pathSegments.length != routeSegments.length) continue;
			ObjectBuilder pathParams = new ObjectBuilder();
			boolean found = true;
			for (int i = 0; i < pathSegments.length; i++) {
				String pathSegment = pathSegments[i];
				String routeSegment = routeSegments[i];
				if (routeSegment.startsWith(":"))
					pathParams.with(routeSegment.substring(1), pathSegment);
				else if (!pathSegment.equals(routeSegment)) {
					found = false;
					break;
				}
			}
			if (!found) continue;
			Method route = routes.get(routeID);
			try {
				return (HTTPResponse) route.invoke(null, pathParams.build());
			} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
				throw new InvocationTargetException(e, "An error occurred executing route " + routeID + ".");
			}
		}
		throw new NameNotFoundException("Could not find route " + method + " " + path + ".");
	}

	protected void send(HttpExchange exchange, HTTPResponse response) {
		byte[] bytes;
		if (response.getParsingMode() == ParsingMode.JSON) {
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			bytes = response.toString().getBytes(StandardCharsets.UTF_8);
		} else if (response.getParsingMode() == ParsingMode.RAW)
			bytes = (byte[]) response.getData();
		else bytes = ((String) response.getData()).getBytes(StandardCharsets.UTF_8);
		try {
			exchange.sendResponseHeaders(response.getCode(), bytes.length);
			OutputStream os = exchange.getResponseBody();
			os.write(bytes);
			os.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public HTTPServer handlers(Class<?>... handlers) {
		for (Class<?> handler : handlers) {
			String routerPath = "";
			if (handler.isAnnotationPresent(Router.class))
				routerPath = handler.getAnnotation(Router.class).value();
			List<Method> routes = ReflectionUtil.getMethodsAnnotatedWith(handler, Route.class);
			for (Method route : routes) {
				Route info = route.getAnnotation(Route.class);
				String path = routerPath + info.path();
				String id = info.method() + " " + path;
				if (!Modifier.isStatic(route.getModifiers())) {
					handleRouteRegistration(id, "Route method is not static.");
					continue;
				}
				if (route.getReturnType() != HTTPResponse.class) {
					handleRouteRegistration(id, "Route method must return HTTPResponse.");
					continue;
				}
				// TODO check for method signature
//				if (!validatePath(path)) {
//					handleRouteRegistrationError(id, "Route path is not valid.");
//					continue;
//				}
				if (this.routes.containsKey(id)) {
					handleRouteRegistration(id, "Another route is already defined at the specified path.");
					continue;
				}
				this.routes.put(id, route);
				handleRouteRegistration(id, null);
			}
		}
		return this;
	}

	public void start() {
		server.start();
	}

	public void stop() {
		server.stop(0);
	}

	public HttpServer getServer() {
		return server;
	}

	public Map<String, Method> getRoutes() {
		return routes;
	}

	public HTTPResponse getNotFound() {
		return HTTPResponse.NOT_FOUND;
	}

	public HTTPResponse getInternalError() {
		return HTTPResponse.INTERNAL_ERROR;
	}

	protected void handleRequestResponse(HttpExchange exchange, HTTPResponse response, Exception e) {
		System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI());
		if (e != null) e.printStackTrace();
	}

	protected void handleRouteRegistration(String routeID, String error) {
		if (error == null) System.out.println(routeID + " SUCCESS");
		else System.out.println(routeID + " FAILED: " + error);
	}
}
