package dev.petshopsoftware.utilities.HTTP.Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import dev.petshopsoftware.utilities.JSON.JSON;
import dev.petshopsoftware.utilities.Util.ParsingMode;
import dev.petshopsoftware.utilities.Util.StringUtils;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HTTPData {
	private final HttpExchange exchange;
	private final HTTPServer server;
	private final String requestID;
	private final HTTPMethod method;
	private final String path;
	private final Route route;
	private final Map<String, String> pathParams;
	private final Map<String, String> queryParams;
	private final Headers headers;
	protected Exception bodyParseError = null;
	private String ip;
	private byte[] rawBody;
	private String body;
	private JsonNode jsonBody;

	public HTTPData(HttpExchange exchange, HTTPServer server, String requestID, HTTPMethod method, String path, Route route, Map<String, String> pathParams, Map<String, String> queryParams, byte[] rawBody) {
		this.exchange = exchange;
		this.server = server;
		this.requestID = requestID;
		this.method = method;
		this.path = path;
		this.route = route;
		this.pathParams = pathParams;
		this.queryParams = queryParams;
		this.headers = exchange.getRequestHeaders();
		this.parseIP(exchange);
		this.parseBody(rawBody);
	}

	private void parseBody(byte[] rawBody) {
		if (rawBody == null) rawBody = new byte[0];
		this.rawBody = rawBody;
		this.body = new String(rawBody, StandardCharsets.UTF_8);
		if (this.route != null) {
			if (this.route.parsingMode() == ParsingMode.JSON)
				try {
					this.jsonBody = JSON.MAPPER.readTree(this.body);
				} catch (JsonProcessingException e) {
					this.bodyParseError = e;
				}
		}

	}

	private void parseIP(HttpExchange exchange) {
		String ipAddress = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
		if (ipAddress == null)
			this.ip = exchange.getRemoteAddress().getAddress().getHostAddress();
		else this.ip = ipAddress;
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

	public String path() {
		return path;
	}

	public Route route() {
		return route;
	}

	public String ip() {
		return ip;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(method).append(" ").append(path).append(" from ").append(ip).append("\n");
		builder.append("REQUEST ID: ").append(requestID).append("\n");
		if (!queryParams.isEmpty()) {
			builder.append("Query:\n");
			StringBuilder queryBuilder = new StringBuilder();
			queryParams.forEach((key, value) -> queryBuilder.append(key).append(": ").append(value).append("\n"));
			builder.append(StringUtils.padLeft(queryBuilder.toString(), 2)).append("\n");
		}
		if (!headers.isEmpty()) {
			builder.append("Headers:\n");
			StringBuilder headerBuilder = new StringBuilder();
			headers.forEach((key, values) -> values.forEach(value -> headerBuilder.append(key).append(": ").append(value).append("\n")));
			builder.append(StringUtils.padLeft(headerBuilder.toString(), 2)).append("\n");
		}
		if (rawBody.length > 0) {
			builder.append("Body:\n");
			StringBuilder bodyBuilder = new StringBuilder();
			if (this.route != null && bodyParseError == null) {
				if (route.parsingMode() == ParsingMode.RAW)
					bodyBuilder.append(DatatypeConverter.printHexBinary(rawBody));
				else if (route.parsingMode() == ParsingMode.STRING)
					bodyBuilder.append(body);
				else if (route.parsingMode() == ParsingMode.JSON)
					bodyBuilder.append(jsonBody.toPrettyString());
				else bodyBuilder.append("Invalid Body: Invalid route Parsing Mode.");
			} else if (route == null) bodyBuilder.append(body);
			else bodyBuilder.append("Invalid Body: ").append(bodyParseError.getMessage());
			builder.append(StringUtils.padLeft(bodyBuilder.toString(), 2));
		}
		return builder.toString();
	}
}
