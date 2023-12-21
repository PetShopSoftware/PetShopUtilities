package dev.petshopsoftware.utilities.HTTP.Server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import dev.petshopsoftware.utilities.JSON.JSON;
import dev.petshopsoftware.utilities.Util.ParsingMode;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPData {
	private final Route route;
	@JsonIgnore
	private final HttpExchange exchange;
	@JsonIgnore
	private final HTTPServer server;
	private final Map<String, String> pathParams;
	private byte[] rawBody;
	private String body;
	private JsonNode jsonBody;
	private Map<String, String> queryParams;
	private Map<String, List<String>> headers;

	public HTTPData(Route route, HttpExchange exchange, HTTPServer server, Map<String, String> pathParams, byte[] rawBody) {
		this.route = route;
		this.exchange = exchange;
		this.server = server;
		this.pathParams = pathParams;
		this.parseBody(rawBody);
		this.parseHeaders(exchange);
		this.parseQuery(exchange);
	}

	private void parseBody(byte[] rawBody) {
		this.rawBody = rawBody;
		this.body = new String(rawBody);
		try {
			this.jsonBody = JSON.MAPPER.readTree(this.body);
		} catch (JsonProcessingException e) {
			if (this.route.parsingMode() == ParsingMode.JSON)
				throw new UnsupportedOperationException("Invalid JSON body.", e);
			else this.jsonBody = null;
		}
	}

	private void parseHeaders(HttpExchange exchange) {
		Headers requestHeaders = exchange.getRequestHeaders();
		Map<String, List<String>> headers = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
			String headerKey = entry.getKey();
			List<String> headerValues = entry.getValue();
			headers.put(headerKey.toLowerCase(), headerValues);
		}
		this.headers = headers;
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

	public Map<String, List<String>> headers() {
		return headers;
	}

	public HTTPServer server() {
		return server;
	}

	public HttpExchange exchange() {
		return exchange;
	}

	public Route route() {
		return route;
	}
}
