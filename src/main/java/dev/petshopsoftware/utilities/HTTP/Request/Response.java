package dev.petshopsoftware.utilities.HTTP.Request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.petshopsoftware.utilities.JSON.JSON;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Response {
	private final CloseableHttpClient client;
	private final HttpRequestBase request;
	private final HttpResponse response;
	private final int statusCode;
	private final String statusMessage;
	private byte[] rawBody;
	private String body;
	private JsonNode jsonBody;

	public Response(CloseableHttpClient client, HttpRequestBase request, HttpResponse response) {
		this.client = client;
		this.request = request;
		this.response = response;

		this.statusCode = response.getStatusLine().getStatusCode();
		this.statusMessage = response.getStatusLine().getReasonPhrase();

		this.rawBody();
		this.body();
	}

	public CloseableHttpClient getClient() {
		return client;
	}

	public HttpRequestBase getRequest() {
		return request;
	}

	public HttpResponse getResponse() {
		return response;
	}

	public int statusCode() {
		return this.statusCode;
	}

	public String statusMessage() {
		return this.statusMessage;
	}

	public byte[] rawBody() {
		if (this.rawBody != null) return rawBody;
		try {
			this.rawBody = EntityUtils.toByteArray(response.getEntity());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this.rawBody;
	}

	public String body() {
		if (this.body != null) return this.body;
		try {
			this.body = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			this.body = new String(this.rawBody(), StandardCharsets.UTF_8);
		}
		return this.body;
	}

	public JsonNode jsonBody() {
		if (this.jsonBody != null) return this.jsonBody;
		try {
			this.jsonBody = JSON.MAPPER.readTree(this.body().trim());
			return this.jsonBody;
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		StringBuilder message = new StringBuilder();
		message.append(request.getMethod()).append(" ").append(request.getURI().toString()).append("\n");
//		message.append("Request:").append("\n");
//            connection.getRequestProperties().forEach((key, values) ->
//                    values.forEach(value -> message.append("  ").append(key).append(": ").append(value).append("\n")));
//            try {
//                String prettyBody = JSON.MAPPER.readTree(request.lastBody).toPrettyString();
//                for (String line : prettyBody.split("\n"))
//                    message.append("  ").append(line).append("\n");
//            } catch (JsonProcessingException e) {
//                message.append("  ").append(request.lastBody);
//            }
//            message.append("Response:").append("\n");
		message.append("  ").append(statusCode).append(" ").append(statusMessage).append("\n");
		for (Header header : response.getAllHeaders())
			message.append("  ").append(header.getName()).append(": ").append(header.getValue()).append("\n");
		try {
			String prettyBody = jsonBody().toPrettyString();
			for (String line : prettyBody.split("\n"))
				message.append("  ").append(line).append("\n");
		} catch (Exception e) {
			message.append("  ").append(body());
		}
		return message.toString();
	}
}
