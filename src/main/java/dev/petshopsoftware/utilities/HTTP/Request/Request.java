package dev.petshopsoftware.utilities.HTTP.Request;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.MessageLite;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPMethod;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;

public class Request {
	private final HttpURLConnection connection;

	public Request(String url, String proxy) {
		try {
			URL connectionURL = new URI(url).toURL();
			if (proxy == null)
				connection = (HttpURLConnection) connectionURL.openConnection();
			else {
				String[] proxyParts = proxy.split(":");
				connection = (HttpURLConnection) connectionURL.openConnection(
						new Proxy(
								Proxy.Type.HTTP,
								new InetSocketAddress(proxyParts[0], Integer.parseInt(proxyParts[1]))
						)
				);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Request(String url) {
		this(url, null);
	}

	public Request method(HTTPMethod method) {
		try {
			connection.setRequestMethod(method.name());
		} catch (ProtocolException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public Request trustAllCerts() {
		if (!(connection instanceof HttpsURLConnection))
			return this;

		TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}
		};

		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			((HttpsURLConnection) connection).setHostnameVerifier(allHostsValid);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public Request certificate(String alias, X509Certificate certificate) {
		if (!(connection instanceof HttpsURLConnection))
			throw new RuntimeException("Certificates are not supported by HttpURLConnection.");
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, null);
			keyStore.setCertificateEntry(alias, certificate);

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, null);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());


			((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public Request header(String key, String value) {
		connection.setRequestProperty(key, value);
		return this;
	}

	public Request cookie(String key, String value) {
		String cookies = connection.getRequestProperty("cookie");
		if (cookies == null) cookies = "";
		else cookies += "; ";
		cookies += key + "=" + value;
		return header("cookie", cookies);
	}

	public Request userAgent(String userAgent) {
		return header("User-Agent", userAgent);
	}

	public Request contentType(String contentType) {
		return header("Content-Type", contentType);
	}

	public Request authentication(String credential) {
		return header("Authorization", credential);
	}

	public Request authentication(String method, String credential) {
		return authentication(method + " " + credential);
	}

	public Request body(byte[] body) {
		connection.setDoOutput(true);
		try (OutputStream os = connection.getOutputStream()) {
			os.write(body, 0, body.length);
			os.flush();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public Request body(String body) {
		return body(body.getBytes());
	}

	public Request body(JsonNode body) {
		return body(body.toPrettyString());
	}

	public Request body(MessageLite message) {
		return body(message.toByteArray());
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
		body(postDataBytes);
		return this;
	}

	public HttpURLConnection getConnection() {
		return connection;
	}

	public Response execute() throws RequestException {
		Response response = new Response(this);
		if (response.statusCode() < 200 || response.statusCode() > 399)
			throw new RequestException(response);
		return response;
	}
}
