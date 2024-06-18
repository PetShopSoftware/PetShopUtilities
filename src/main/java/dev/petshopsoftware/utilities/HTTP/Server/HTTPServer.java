package dev.petshopsoftware.utilities.HTTP.Server;

import com.sun.net.httpserver.*;
import dev.petshopsoftware.utilities.JSON.ObjectBuilder;
import dev.petshopsoftware.utilities.Logging.LogMessage;
import dev.petshopsoftware.utilities.Logging.Logger;
import dev.petshopsoftware.utilities.Util.InputChecker.InvalidInputException;
import dev.petshopsoftware.utilities.Util.ParsingMode;
import dev.petshopsoftware.utilities.Util.RandomUtil;
import dev.petshopsoftware.utilities.Util.ReflectionUtil;
import dev.petshopsoftware.utilities.Util.StringUtils;
import dev.petshopsoftware.utilities.Util.Types.Pair;
import dev.petshopsoftware.utilities.Util.Types.Quad;
import org.apache.http.impl.EnglishReasonPhraseCatalog;

import javax.naming.NameNotFoundException;
import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class HTTPServer {
	private final String id;
	private final String subdomain;
	private final String domain;
	private final Logger logger;
	private final int port;
	private final HttpServer server;
	private final Map<String, Pair<Route, Method>> routes = new HashMap<>();
	private final List<HTTPHandler> handlers = new LinkedList<>();
	private List<String> sortedRoutes = new ArrayList<>();

	public HTTPServer(String subdomain, String domain, int port, boolean ssl) {
		try {
			if (ssl)
				this.server = HttpsServer.create(new InetSocketAddress(port), 0);
			else
				this.server = HttpServer.create(new InetSocketAddress(port), 0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
		this.id = (subdomain == null ? RandomUtil.generateIdentifier(8) : subdomain);
		this.subdomain = subdomain;
		this.domain = domain;
		this.port = port;
		this.logger = Logger.get("http-" + id);
		init();
	}

	public HTTPServer(String subdomain, String domain, int port) {
		this(subdomain, domain, port, true);
	}

	public HTTPServer(int port, boolean ssl) {
		this(null, null, port, ssl);
	}

	protected void init() {
		try {
			this.setupNGINX();
		} catch (Exception e) {
			logger.error(LogMessage.fromException(e));
		}
		server.createContext("/", this::handleRequest);
		logger.info("Server " + id + " initialized successfully.");
	}

	protected void setupNGINX() throws Exception {
		if (subdomain == null || domain == null) return;
		try {
			NGINXUtil.setupServerBlock(subdomain, domain, port);
			logger.info("NGINX setup successfully.");
		} catch (Exception e) {
			throw new Exception("NGINX could not be setup successfully.", e);
		}
	}

	protected void handleRequest(HttpExchange exchange) {
		String requestID = RandomUtil.generateIdentifier(32);
		HTTPData data = null;
		HTTPResponse response = null;
		try {
			String path = exchange.getRequestURI().getPath();
			HTTPMethod method;
			try {
				method = HTTPMethod.valueOf(exchange.getRequestMethod());
			} catch (IllegalArgumentException e) {
				method = null;
			}
			Map<String, String> queryParams;
			try {
				queryParams = parseQuery(exchange.getRequestURI().getQuery());
			} catch (UnsupportedEncodingException e) {
				queryParams = null;
			}
			Route route;
			Method invoked;
			Map<String, String> pathParams;
			try {
				Quad<String, Route, Method, Map<String, String>> resolvedRoute = resolveRoute(method, path);
				route = resolvedRoute.getV2();
				invoked = resolvedRoute.getV3();
				pathParams = resolvedRoute.getV4();
			} catch (NameNotFoundException e) {
				route = null;
				invoked = null;
				pathParams = null;
			}
			byte[] rawBody;
			try {
				rawBody = readBody(exchange);
			} catch (IOException e) {
				rawBody = null;
			}
			data = new HTTPData(exchange, this, requestID, method, path, route, pathParams, queryParams, rawBody);
			logger.info("IN: " + data.method() + " " + data.path() + " from " + data.ip() + ".");
			logger.debug(data.toString());
			if (data.method() == null)
				response = getInvalidMethod(data);
			else if (data.queryParams() == null)
				response = getInvalidQuery(data);
			else if (data.bodyParseError != null)
				response = getInvalidBody(data);
			else if (data.method() == HTTPMethod.OPTIONS)
				response = getOptionsResponse(data);
			else if (data.route() == null || invoked == null || data.pathParams() == null)
				response = getNotFound(data);
			else {
				for (HTTPHandler handler : handlers) {
					if (!handler.matchesRoute(data, route, invoked)) continue;
					HTTPResponse handlerResponse = handler.handle(data, invoked);
					if (handlerResponse != null) {
						response = handlerResponse;
						break;
					}
				}
				if (response == null)
					try {
						response = (HTTPResponse) invoked.invoke(null, data);
					} catch (Exception e) {
						if (e.getCause() instanceof InvalidInputException)
							response = HTTPResponse.BAD_REQUEST.message(e.getCause().getMessage());
						else if (e.getCause() instanceof HTTPResponseException)
							response = ((HTTPResponseException) e.getCause()).getResponse();
						else throw new RuntimeException("An internal error occured.", e);
					}
			}
			if (response == null) throw new Exception("Failed to assign request response.");
		} catch (Exception e) {
			Exception ex = e;
			if (data == null) ex = new RuntimeException("Failed to capture request data.", e);
			logger.error(new RuntimeException("An exception occurred while handling request.", ex));
			response = getInternalError(null);
		}
		response.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", "*")
				.header("Access-Control-Allow-Headers", "*")
				.header("Access-Control-Max-Age", "86400")
				.header("X-Request-ID", requestID);
		try {
			byte[] bytes = sendResponse(exchange, response);
			if (data != null) logger.info("OUT: " + data.method() + " " + data.path() + " to " + data.ip() + ".");
			else logger.warn("OUT: Failed to capture request data for " + requestID + ".");
			StringBuilder builder = new StringBuilder();
			builder.append(exchange.getResponseCode()).append(" ").append(EnglishReasonPhraseCatalog.INSTANCE.getReason(exchange.getResponseCode(), Locale.ENGLISH)).append("\n");
			builder.append("REQUEST ID: ").append(requestID).append("\n");
			if (!exchange.getRequestHeaders().isEmpty()) {
				builder.append("Headers:\n");
				StringBuilder headerBuilder = new StringBuilder();
				exchange.getResponseHeaders().forEach((key, values) -> values.forEach(value -> headerBuilder.append(key).append(": ").append(value).append("\n")));
				builder.append(StringUtils.padLeft(headerBuilder.toString(), 2)).append("\n");
			}
			if (bytes.length > 0) {
				builder.append("Body:\n");
				StringBuilder bodyBuilder = new StringBuilder();
				if (response.getParsingMode() == ParsingMode.RAW)
					bodyBuilder.append(DatatypeConverter.printHexBinary(bytes));
				else bodyBuilder.append(new String(bytes));
				builder.append(StringUtils.padLeft(bodyBuilder.toString(), 2));
			}
			logger.debug(builder.toString());
		} catch (IOException e) {
			logger.error(new RuntimeException("Could not send response to client.", e));
		}
	}

	protected Quad<String, Route, Method, Map<String, String>> resolveRoute(HTTPMethod method, String path) throws NameNotFoundException {
		String[] pathSegments = path.split("/");
		for (String routeID : sortedRoutes) {
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
			return new Quad<>(routeID.split(" ")[1], routeInfo, route, pathParams);
		}
		throw new NameNotFoundException("Could not find route " + method + " " + path + ".");
	}

	protected Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
		Map<String, String> result = new HashMap<>();
		if (query == null || query.isEmpty()) {
			return result;
		}
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
			result.put(key, value);
		}
		return result;

	}

	protected byte[] readBody(HttpExchange exchange) throws IOException {
		InputStream inputStream = exchange.getRequestBody();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int len;
		byte[] data = new byte[1024];
		while ((len = inputStream.read(data, 0, data.length)) != -1)
			buffer.write(data, 0, len);
		buffer.flush();
		return buffer.toByteArray();
	}

	protected void send(HttpExchange exchange, int code, byte[] bytes) throws IOException {
		if (bytes == null) bytes = new byte[0];
		exchange.sendResponseHeaders(code, bytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(bytes);
		os.close();
	}

	protected byte[] sendResponse(HttpExchange exchange, HTTPResponse response) throws IOException {
		byte[] bytes = null;
		if (response.getParsingMode() == ParsingMode.RAW)
			bytes = (byte[]) response.getData();
		else if (response.getParsingMode() == ParsingMode.STRING)
			bytes = ((String) response.getData()).getBytes(StandardCharsets.UTF_8);
		else if (response.getParsingMode() == ParsingMode.JSON) {
			bytes = response.toString().getBytes(StandardCharsets.UTF_8);
			if (!response.getHeaders().containsKey("Content-Type"))
				response.header("Content-Type", "application/json");
		}
		exchange.getResponseHeaders().putAll(response.getHeaders());
		send(exchange, response.getCode(), bytes);
		return bytes;
	}

	protected void sortRoutes() {
		sortedRoutes = routes.keySet().stream().sorted((r1, r2) -> {
			int c1 = (int) r1.chars().filter(c -> c == ':').count();
			int c2 = (int) r2.chars().filter(c -> c == ':').count();
			return Integer.compare(c1, c2);
		}).collect(Collectors.toList());
	}

	public HTTPServer routers(String basePath, Class<?>... routers) {
		for (Class<?> router : routers) {
			String routerPath = "";
			if (router.isAnnotationPresent(Router.class))
				routerPath = router.getAnnotation(Router.class).value();
			List<Method> routes = ReflectionUtil.getMethodsAnnotatedWith(router, Route.class);
			for (Method route : routes) {
				Route info = route.getAnnotation(Route.class);
				String path = basePath + routerPath + info.path();
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
		sortRoutes();
		return this;
	}

	public HTTPServer routers(Class<?>... routers) {
		return routers("", routers);
	}

	public HTTPServer handlers(HTTPHandler... handlers) {
		this.handlers.addAll(Arrays.asList(handlers));
		return this;
	}

	public HTTPServer configure(HttpsConfigurator httpsConfigurator) {
		if (this.server instanceof HttpsServer)
			((HttpsServer) this.server).setHttpsConfigurator(httpsConfigurator);
		else
			throw new RuntimeException("This HTTPServer does not support this action.");
		return this;
	}

	public HTTPServer trustAllCerts(InputStream pfxFile, String password, boolean userAuth) {
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(pfxFile, password.toCharArray());

			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password.toCharArray());

			TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						public void checkClientTrusted(X509Certificate[] chain, String authType) {
						}

						public X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}

						public void checkServerTrusted(X509Certificate[] certs, String authType) {
						}
					}
			};

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustAllCerts, new SecureRandom());

			return this.configure(new HttpsConfigurator(sslContext) {
				@Override
				public void configure(HttpsParameters params) {
					SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
					sslParameters.setNeedClientAuth(userAuth);
					params.setSSLParameters(sslParameters);
				}
			});
		} catch (Exception e) {
			this.logger.error(LogMessage.fromException(e));
		}
		return this;
	}

	public boolean validatePath(String path) {
		return true; // TODO method logic
	}

	public void start() {
		server.start();
		logger.info("Server started successfully at http://localhost:" + port + (subdomain != null && domain != null ? " (https://" + subdomain + "." + domain + ")" : "") + ".");
	}

	public void stop() {
		server.stop(0);
		logger.info("Server stopped successfully.");
	}

	public String getID() {
		return id;
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

	public HTTPResponse getNotFound(HTTPData data) {
		URI uri = data.exchange().getRequestURI();
		String routeBuilder = data.exchange().getRequestMethod() + " " + uri.getPath();
		return HTTPResponse.NOT_FOUND.data(
				new ObjectBuilder()
						.with("path", routeBuilder)
						.build()
		);
	}

	public HTTPResponse getInvalidMethod(HTTPData data) {
		return new HTTPResponse().code(405).message("Invalid HTTP method " + data.method() + ".");
	}

	public HTTPResponse getInvalidBody(HTTPData data) {
		if (data.route() != null && data.route().parsingMode() == ParsingMode.JSON)
			return HTTPResponse.INVALID_JSON;
		return HTTPResponse.INVALID_BODY;
	}

	public HTTPResponse getInvalidQuery(HTTPData data) {
		return HTTPResponse.BAD_REQUEST.message("Invalid query.");
	}

	public HTTPResponse getInternalError(HTTPData data) {
		return HTTPResponse.INTERNAL_ERROR;
	}

	public HTTPResponse getOptionsResponse(HTTPData data) {
		return new HTTPResponse(200, new byte[0]);
	}
}
