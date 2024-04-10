package dev.petshopsoftware.utilities.HTTP.Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.*;
import dev.petshopsoftware.utilities.Logging.Log;
import dev.petshopsoftware.utilities.Logging.Logger;
import dev.petshopsoftware.utilities.Util.InputChecker.InvalidInputException;
import dev.petshopsoftware.utilities.Util.ParsingMode;
import dev.petshopsoftware.utilities.Util.RandomUtil;
import dev.petshopsoftware.utilities.Util.ReflectionUtil;
import dev.petshopsoftware.utilities.Util.Types.Pair;
import dev.petshopsoftware.utilities.Util.Types.Quad;

import javax.naming.NameNotFoundException;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
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

	public HTTPServer(int port) {
		this(null, null, port, true);
	}

	protected void init() {
		try {
			this.setupNGINX();
		} catch (Exception e) {
			logger.error(Log.fromException(e));
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
		String requestID = UUID.randomUUID().toString();
		HTTPMethod method = HTTPMethod.valueOf(exchange.getRequestMethod());
		String path = exchange.getRequestURI().toString();
		Quad<String, Route, Method, Map<String, String>> routeData = null;
		HTTPResponse response = null;

		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
		exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
		if (method == HTTPMethod.OPTIONS) {
			try {
				send(exchange, 200, new byte[0]);
			} catch (IOException e) {
				logger.error(Log.fromException(new RuntimeException("Could not send OPTIONS response to client.", e)));
			}
			return;
		}

		try {
			routeData = resolveRoute(method, path);
			HTTPData data = new HTTPData(method, requestID, routeData.getV1(), routeData.getV2(), exchange, this, routeData.getV4(), readBody(exchange));
			logger.debug(data.toString());
			for (HTTPHandler handler : handlers) {
				if (!handler.matchesRoute(data, routeData.getV2(), routeData.getV3())) continue;
				HTTPResponse handlerResponse = handler.handle(data, routeData.getV3());
				if (handlerResponse != null) {
					response = handlerResponse;
					break;
				}
			}
			if (response == null)
				response = (HTTPResponse) routeData.getV3().invoke(null, data);
		} catch (NameNotFoundException e) {
			response = getNotFound();
		} catch (JsonProcessingException e) {
			response = getBadRequest(routeData.getV2());
		} catch (Exception e) {
			if (e.getCause() instanceof InvalidInputException)
				response = getBadRequest(routeData == null ? null : routeData.getV2()).message(e.getCause().getMessage());
			else if (e.getCause() instanceof HTTPResponseException)
				response = ((HTTPResponseException) e.getCause()).getResponse();
			else {
				response = getInternalError(routeData == null ? null : routeData.getV2());
				logger.error(Log.fromException(new RuntimeException("An internal error occurred.", e)));
			}
		}
		try {
			byte[] bytes;
			if (response.getParsingMode() == ParsingMode.JSON) {
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				bytes = response.toString().getBytes(StandardCharsets.UTF_8);
			} else if (response.getParsingMode() == ParsingMode.RAW)
				bytes = (byte[]) response.getData();
			else bytes = ((String) response.getData()).getBytes(StandardCharsets.UTF_8);
			send(exchange, response.getCode(), bytes);
			logger.debug(requestID + " Response " + response.getCode() + "\n"
					+ exchange.getResponseHeaders().entrySet().stream().map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue())).collect(Collectors.joining("\n")) + "\n"
					+ (response.getParsingMode() == ParsingMode.RAW ? HexFormat.of().formatHex(bytes) : new String(bytes)));
		} catch (IOException e) {
			logger.error(Log.fromException(new RuntimeException("Could not send response to client.", e)));
		}
	}

	protected Quad<String, Route, Method, Map<String, String>> resolveRoute(HTTPMethod method, String path) throws NameNotFoundException {
		String deparameterizedPath = path.split("\\?")[0];
		String[] pathSegments = deparameterizedPath.split("/");
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
		throw new NameNotFoundException("Could not find route " + method + " " + deparameterizedPath + ".");
	}

	protected byte[] readBody(HttpExchange exchange) throws IOException {
		return exchange.getRequestBody().readAllBytes();
	}

	protected void send(HttpExchange exchange, int code, byte[] bytes) throws IOException {
		exchange.sendResponseHeaders(code, bytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(bytes);
		os.close();
	}

	protected void sortRoutes() {
		sortedRoutes = routes.keySet().stream().sorted((r1, r2) -> {
			int c1 = (int) r1.chars().filter(c -> c == ':').count();
			int c2 = (int) r2.chars().filter(c -> c == ':').count();
			return Integer.compare(c1, c2);
		}).toList();
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
		this.handlers.addAll(List.of(handlers));
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
					SSLEngine engine = sslContext.createSSLEngine();
					params.setNeedClientAuth(userAuth);
					params.setCipherSuites(engine.getEnabledCipherSuites());
					params.setProtocols(engine.getEnabledProtocols());
					SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
					params.setSSLParameters(defaultSSLParameters);
				}
			});
		} catch (Exception e) {
			this.logger.error(Log.fromException(e));
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
