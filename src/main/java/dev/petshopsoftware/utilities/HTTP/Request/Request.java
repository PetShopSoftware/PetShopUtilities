package dev.petshopsoftware.utilities.HTTP.Request;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.MessageLite;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPMethod;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

public class Request {
	private final Map<String, X509Certificate> certificates = new HashMap<>();
	private final Map<String, List<String>> headers = new HashMap<>();
	private String url;
	private String proxy;
	private HTTPMethod method = null;
	private boolean trustAllCerts = false;
	private boolean disallowRedirects = false;
	private byte[] body = null;

	public Request(String url) {
		this.url(url);
	}

	public Request url(String url) {
		this.url = url;
		return this;
	}

	public Request proxy(String proxy) {
		this.proxy = proxy;
		return this;
	}

	public Request method(HTTPMethod method) {
		this.method = method;
		return this;
	}

	public Request trustAllCerts(boolean override) {
		this.trustAllCerts = override;
		return this;
	}

	public Request trustAllCerts() {
		return this.trustAllCerts(true);
	}

	public Request disallowRedirects(boolean override) {
		this.disallowRedirects = override;
		return this;
	}

	public Request disallowRedirects() {
		return this.disallowRedirects(true);
	}

	public Request certificate(String alias, X509Certificate certificate) {
		this.certificates.put(alias, certificate);
		return this;
	}

	public Request header(String key, String value, boolean replace) {
		if (!this.headers.containsKey(key.toLowerCase()))
			this.headers.put(key.toLowerCase(), new LinkedList<>());
		List<String> headerValues = this.headers.get(key.toLowerCase());
		if (replace) headerValues.clear();
		headerValues.add(value);
		return this;
	}

	public Request header(String key, String value) {
		return this.header(key, value, true);
	}

	public Request removeHeader(String key, int index) {
		if (index < 0)
			this.headers.remove(key.toLowerCase());
		else {
			List<String> headerValues = this.headers.get(key.toLowerCase());
			if (headerValues != null)
				headerValues.remove(index);
		}
		return this;
	}

	public Request removeHeader(String key) {
		return this.removeHeader(key, -1);
	}

	public Request cookie(String key, String value) {
		return this.header("cookie", key + "=" + value);
	}

	public Request userAgent(String userAgent) {
		return this.header("User-Agent", userAgent);
	}

	public Request contentType(String contentType) {
		return this.header("Content-Type", contentType);
	}

	public Request authentication(String credential) {
		return this.header("Authorization", credential);
	}

	public Request authentication(String method, String credential) {
		return authentication(method + " " + credential);
	}

	public Request body(byte[] body) {
		this.body = body;
		return this;
	}

	public Request body(String body) {
		return this.body(body.getBytes());
	}

	public Request body(JsonNode body) {
		return this.body(body.toPrettyString());
	}

	public Request body(MessageLite message) {
		return this.body(message.toByteArray());
	}

	public Request form(JsonNode form) {
		StringBuilder postData = new StringBuilder();
		for (Iterator<Map.Entry<String, JsonNode>> it = form.fields(); it.hasNext(); ) {
			Map.Entry<String, JsonNode> field = it.next();
			if (!postData.isEmpty()) postData.append('&');
			postData.append(URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8));
			postData.append('=');
			postData.append(URLEncoder.encode(field.getValue().asText(), StandardCharsets.UTF_8));
		}
		byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
		this.body(postDataBytes);
		return this;
	}

	private CloseableHttpClient createHTTPClient() throws Exception {
		SSLContextBuilder sslBuilder = SSLContextBuilder.create();
		if (this.trustAllCerts)
			sslBuilder.loadTrustMaterial(null, (chain, authType) -> true);
		if (!certificates.isEmpty()) {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, null);
			for (Map.Entry<String, X509Certificate> entry : certificates.entrySet())
				keyStore.setCertificateEntry(entry.getKey(), entry.getValue());
			sslBuilder.loadKeyMaterial(keyStore, null);
		}

		HttpClientBuilder clientBuilder = HttpClients.custom()
				.setSSLSocketFactory(new SSLConnectionSocketFactory(sslBuilder.build()));
		if (proxy != null) {
			String[] proxyParts = proxy.split(":");
			clientBuilder.setProxy(new HttpHost(proxyParts[0], Integer.parseInt(proxyParts[1])));
		}
		if (this.disallowRedirects) {
			clientBuilder.setRedirectStrategy(new DefaultRedirectStrategy() {
				@Override
				protected boolean isRedirectable(String method) {
					return false;
				}
			});
		}
		return clientBuilder.build();
	}

	private HttpRequestBase createHTTPRequest(URI uri) {
		HttpRequestBase httpRequest;
		switch (this.method) {
			case GET -> httpRequest = new HttpGet(uri);
			case POST -> httpRequest = new HttpPost(uri);
			case PUT -> httpRequest = new HttpPut(uri);
			case PATCH -> httpRequest = new HttpPatch(uri);
			case DELETE -> httpRequest = new HttpDelete(uri);
			default -> throw new RuntimeException("HTTPMethod " + this.method + "is not supported.");
		}
		return httpRequest;
	}

	public Response execute() throws RequestException {
		URI uri;
		try {
			uri = new URI(this.url);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		CloseableHttpClient client;
		try {
			client = createHTTPClient();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create HTTP Client", e);
		}

		HttpRequestBase httpRequest = createHTTPRequest(uri);

		this.headers.forEach((key, values) -> values.forEach(value -> httpRequest.addHeader(key, value)));
		if (httpRequest instanceof HttpEntityEnclosingRequestBase && this.body != null)
			((HttpEntityEnclosingRequestBase) httpRequest).setEntity(new ByteArrayEntity(this.body));

		HttpResponse httpResponse;
		try {
			httpResponse = client.execute(httpRequest);
		} catch (IOException e) {
			throw new RuntimeException("Failed to execute HTTP Request to " + uri + ".", e);
		}

		Response response = new Response(client, httpRequest, httpResponse);

		try {
			EntityUtils.consume(httpResponse.getEntity());
			client.close();
		} catch (IOException e) {
			throw new RuntimeException("Failed to finalize HTTP Connection.", e);
		}

		if (response.statusCode() < 200 || response.statusCode() > 399)
			throw new RequestException(response);
		return response;
	}
}
