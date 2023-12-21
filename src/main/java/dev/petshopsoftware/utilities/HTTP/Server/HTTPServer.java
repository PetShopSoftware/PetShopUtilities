package dev.petshopsoftware.utilities.HTTP.Server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.petshopsoftware.utilities.Util.ParsingMode;
import dev.petshopsoftware.utilities.Util.ReflectionUtil;
import dev.petshopsoftware.utilities.Util.Types.Pair;
import dev.petshopsoftware.utilities.Util.Types.Trio;

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
	private final Map<String, Pair<Route, Method>> routes = new HashMap<>();

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
		HTTPResponse response;
		Exception exception = null;
		Trio<Route, Method, Map<String, String>> routeData = null;
		try {
			routeData = resolveRoute(exchange, method, path);
			HTTPData data = new HTTPData(routeData.getV1(), exchange, this, routeData.getV3(), readBody(exchange));
			response = (HTTPResponse) routeData.getV2().invoke(null, data);
		} catch (NameNotFoundException e) {
			response = getNotFound();
			exception = e;
		} catch (UnsupportedOperationException e) {
			response = getBadRequest(routeData == null ? null : routeData.getV1());
			exception = e;
		} catch (RuntimeException | InvocationTargetException | IllegalAccessException | IOException e) {
			response = getInternalError();
			exception = e;
		}
		handleRequestResponse(exchange, response, exception);
		send(exchange, response);
	}

	protected Trio<Route, Method, Map<String, String>> resolveRoute(HttpExchange exchange, HTTPMethod method, String path) throws NameNotFoundException {
		String[] pathSegments = path.split("/");
		for (String routeID : routes.keySet()) {
			String[] routeIDParts = routeID.split(" ");
			HTTPMethod routeMethod = HTTPMethod.valueOf(routeIDParts[0]);
			if (method != routeMethod) continue;
			String[] routeSegments = routeIDParts[1].split("/");
			if (pathSegments.length != routeSegments.length) continue;
			Map<String, String> pathParams = new HashMap<>();
			boolean found = true;
			for (int i = 0; i < pathSegments.length; i++) {
				String pathSegment = pathSegments[i];
				String routeSegment = routeSegments[i];
				if (routeSegment.startsWith(":"))
					pathParams.put(routeSegment.substring(1), pathSegment);
				else if (!pathSegment.equals(routeSegment)) {
					found = false;
					break;
				}
			}
			if (!found) continue;
			try {
				Pair<Route, Method> routeData = routes.get(routeID);
				Route routeInfo = routeData.getV1();
				Method route = routeData.getV2();
				return new Trio<>(routeInfo, route, pathParams);
			} catch (RuntimeException e) {
				throw new RuntimeException("An error occurred while resolving route " + routeID + ".", e);
			}
		}
		throw new NameNotFoundException("Could not find route " + method + " " + path + ".");
	}

	protected byte[] readBody(HttpExchange exchange) throws IOException {
		return exchange.getRequestBody().readAllBytes();
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
				if (route.getParameterCount() != 1 || route.getParameterTypes()[0] != HTTPData.class) {
					handleRouteRegistration(id, "Route method must take HTTPData as parameter.");
					continue;
				}
//				TODO if (!validatePath(path)) {
//					handleRouteRegistrationError(id, "Route path is not valid.");
//					continue;
//				}
				if (this.routes.containsKey(id)) {
					handleRouteRegistration(id, "Another route is already defined at the specified path.");
					continue;
				}
				this.routes.put(id, new Pair<>(info, route));
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

	public Map<String, Pair<Route, Method>> getRoutes() {
		return routes;
	}

	public HTTPResponse getNotFound() {
		return HTTPResponse.NOT_FOUND;
	}

	public HTTPResponse getBadRequest(Route route) {
		if (route != null && route.parsingMode() == ParsingMode.JSON)
			return HTTPResponse.INVALID_JSON;
		return HTTPResponse.INVALID_BODY;
	}

	public HTTPResponse getInternalError() {
		return HTTPResponse.INTERNAL_ERROR;
	}

	protected void handleRequestResponse(HttpExchange exchange, HTTPResponse response, Exception e) {
		System.out.println("Incoming request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
		System.out.println(response);
		if (e != null) e.printStackTrace();
	}

	protected void handleRouteRegistration(String routeID, String error) {
		if (error == null) System.out.println(routeID + " registered successfully.");
		else System.out.println("Error while registering route " + routeID + ": " + error);
	}
}
