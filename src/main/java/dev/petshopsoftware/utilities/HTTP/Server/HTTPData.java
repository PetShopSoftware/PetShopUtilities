package dev.petshopsoftware.utilities.HTTP.Server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import dev.petshopsoftware.utilities.JSON.JSON;
import dev.petshopsoftware.utilities.Util.ParsingMode;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HTTPData {
	private final HTTPMethod method;
	private final String requestID;
	private final String fullPath;
	private final Route route;
	@JsonIgnore
	private final HttpExchange exchange;
	@JsonIgnore
	private final HTTPServer server;
	private final Map<String, String> pathParams;
	private final Headers headers;
	private byte[] rawBody;
	private String body;
	private JsonNode jsonBody;
	private Map<String, String> queryParams;
	private String ip;

	public HTTPData(HTTPMethod method, String requestID, String fullPath, Route route, HttpExchange exchange, HTTPServer server, Map<String, String> pathParams, byte[] rawBody) throws IOException {
		this.method = method;
		this.requestID = requestID;
		this.fullPath = fullPath;
		this.route = route;
		this.exchange = exchange;
		this.server = server;
		this.pathParams = pathParams;
		this.headers = exchange.getRequestHeaders();
		this.parseBody(rawBody);
		this.parseQuery(exchange);
		this.parseIP(exchange);
	}

	private void parseBody(byte[] rawBody) throws JsonProcessingException {
		this.rawBody = rawBody;
		this.body = new String(rawBody);
		try {
			this.jsonBody = JSON.MAPPER.readTree(this.body);
		} catch (JsonProcessingException e) {
			if (this.route.parsingMode() == ParsingMode.JSON)
				throw e;
			else this.jsonBody = null;
		}
	}

	private void parseQuery(HttpExchange exchange) {
		URI uri = exchange.getRequestURI();
		String queryString = uri.getRawQuery();
		Map<String, String> query = new HashMap<>();
		if (queryString != null) {
			String[] pairs = queryString.split("&");
			for (String pair : pairs) {
				int idx = pair.indexOf("=");
				query.put(
						URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
						URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
				);
			}
		}
		this.queryParams = query;
	}

	private void parseIP(HttpExchange exchange) {
		String ipAddress = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
		this.ip = ipAddress == null ? exchange.getRemoteAddress().getAddress().getHostAddress() : ipAddress;
	}

	public byte[] rawBody() {
		return rawBody;
	}

	public String body() {
		return body;
	}

	public JsonNode jsonBody() {
		return jsonBody;
	}

	public Map<String, String> pathParams() {
		return pathParams;
	}

	public Map<String, String> queryParams() {
		return queryParams;
	}

	public Headers headers() {
		return headers;
	}

	public HTTPServer server() {
		return server;
	}

	public HttpExchange exchange() {
		return exchange;
	}

	public HTTPMethod method() {
		return method;
	}

	public String requestID() {
		return requestID;
	}

	public String fullPath() {
		return fullPath;
	}

	public Route route() {
		return route;
	}

	public String ip() {
		return ip;
	}

	@Override
	public String toString() {
		return route.method() + " " + fullPath + " from " + ip + " (" + requestID + ")\n" +
				headers.entrySet().stream().map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue())).collect(Collectors.joining("\n")) + "\n" +
				(jsonBody != null ? jsonBody.toPrettyString() : body);
	}
}
