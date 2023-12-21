package dev.petshopsoftware.utilities.HTTP.Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.petshopsoftware.utilities.Logging.Log;
import dev.petshopsoftware.utilities.Logging.Logger;
import dev.petshopsoftware.utilities.Util.ParsingMode;
import dev.petshopsoftware.utilities.Util.RandomUtil;
import dev.petshopsoftware.utilities.Util.ReflectionUtil;
import dev.petshopsoftware.utilities.Util.Types.Pair;
import dev.petshopsoftware.utilities.Util.Types.Trio;

import javax.naming.NameNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPServer {
	private final String subdomain;
	private final String domain;
	private final Logger logger;
	private final int port;
	private final HttpServer server;
	private final Map<String, Pair<Route, Method>> routes = new HashMap<>();

	public HTTPServer(String subdomain, String domain, int port) {
		try {
			this.server = HttpServer.create(new InetSocketAddress(port), 0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.subdomain = subdomain;
		this.domain = domain;
		this.port = port;
		this.logger = Logger.get("http-" + (subdomain == null ? RandomUtil.generateIdentifier(8) : subdomain));
		init();
	}

	public HTTPServer(int port) {
		this(null, null, port);
	}


	protected void init() {
		try {
			this.setupNGINX();
		} catch (Exception e) {
			logger.error(Log.fromException(e));
		}
		server.createContext("/", this::handleRequest);
	}

	protected void setupNGINX() throws Exception {
		if (subdomain == null || domain == null) return;
		try {
			NGINXUtil.setupServerBlock(subdomain, domain, port);
		} catch (Exception e) {
			throw new Exception("NGINX could not be setup successfully.", e);
		}
	}

	protected void handleRequest(HttpExchange exchange) {
		HTTPMethod method = HTTPMethod.valueOf(exchange.getRequestMethod());
		String path = exchange.getRequestURI().toString();

		HTTPResponse response;
		Trio<Route, Method, Map<String, String>> routeData = null;
		try {
			routeData = resolveRoute(method, path);
			HTTPData data = new HTTPData(routeData.getV1(), exchange, this, routeData.getV3(), readBody(exchange));
			// TODO log request
			response = (HTTPResponse) routeData.getV2().invoke(null, data);
		} catch (NameNotFoundException e) {
			response = getNotFound();
			logger.error(Log.fromException(e));
		} catch (JsonProcessingException e) {
			response = getBadRequest(routeData.getV1());
		} catch (Exception e) {
			response = getInternalError(routeData == null ? null : routeData.getV1());
			logger.error(Log.fromException(new RuntimeException("An internal error occurred.", e)));
		}

		try {
			send(exchange, response);
		} catch (IOException e) {
			logger.error(Log.fromException(new RuntimeException("Could not send response to client (is connection closed?).", e)));
		}
	}

	protected Trio<Route, Method, Map<String, String>> resolveRoute(HTTPMethod method, String path) throws NameNotFoundException {
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
			Pair<Route, Method> routeData = routes.get(routeID);
			Route routeInfo = routeData.getV1();
			Method route = routeData.getV2();
			return new Trio<>(routeInfo, route, pathParams);
		}
		throw new NameNotFoundException("Could not find route " + method + " " + path + ".");
	}

	protected byte[] readBody(HttpExchange exchange) throws IOException {
		return exchange.getRequestBody().readAllBytes();
	}

	protected void send(HttpExchange exchange, HTTPResponse response) throws IOException {
		byte[] bytes;
		if (response.getParsingMode() == ParsingMode.JSON) {
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			bytes = response.toString().getBytes(StandardCharsets.UTF_8);
		} else if (response.getParsingMode() == ParsingMode.RAW)
			bytes = (byte[]) response.getData();
		else bytes = ((String) response.getData()).getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(response.getCode(), bytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(bytes);
		os.close();
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
					logger.error("Cannot register route " + id + ": method is not static.");
					continue;
				}
				if (route.getReturnType() != HTTPResponse.class) {
					logger.error("Cannot register route " + id + ": method must return HTTPResponse.");
					continue;
				}
				if (route.getParameterCount() != 1 || route.getParameterTypes()[0] != HTTPData.class) {
					logger.error("Cannot register route " + id + ": method must only take HTTPData as parameter.");
					continue;
				}
				if (!validatePath(path)) {
					logger.error("Cannot register route " + id + ": path is invalid.");
					continue;
				}
				if (this.routes.containsKey(id)) {
					logger.error("Cannot register route " + id + ": another route is already defined at the specified path.");
					continue;
				}
				this.routes.put(id, new Pair<>(info, route));
				logger.info("Route " + id + " registered successfully.");
			}
		}
		return this;
	}

	public boolean validatePath(String path) {
		return true; // TODO method logic
	}

	public void start() {
		server.start();
	}

	public void stop() {
		server.stop(0);
	}

	public String getSubdomain() {
		return subdomain;
	}

	public String getDomain() {
		return domain;
	}

	public int getPort() {
		return port;
	}

	public Logger getLogger() {
		return logger;
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

	public HTTPResponse getInternalError(Route route) {
		return HTTPResponse.INTERNAL_ERROR;
	}
}
