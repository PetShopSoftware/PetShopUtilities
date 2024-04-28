package dev.petshopsoftware.utilities.HTTP.Request;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.MessageLite;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPMethod;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class Request {
	static {
		try {
			Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

			methodsField.setAccessible(true);

			String[] oldMethods = (String[]) methodsField.get(null);
			Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
			methodsSet.addAll(Arrays.stream(HTTPMethod.values()).map(Enum::name).collect(Collectors.toSet()));
			String[] newMethods = methodsSet.toArray(new String[0]);

			methodsField.set(null, newMethods);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private final Map<String, X509Certificate> certificates = new HashMap<>();
	private final HttpURLConnection connection;
	private boolean trustAllCerts = false;
	private byte[] body = null;

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
		this.trustAllCerts = true;
		return this;
	}

	public Request certificate(String alias, X509Certificate certificate) {
		if (!(connection instanceof HttpsURLConnection))
			throw new RuntimeException("Certificates are not supported by HttpURLConnection.");
		this.certificates.put(alias, certificate);
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
		this.body = body;
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

	private void configureSSLContext() throws RequestException {
		if (!(connection instanceof HttpsURLConnection))
			return;

		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			KeyManager[] keyManagers = null;
			if (!certificates.isEmpty()) {
				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				keyStore.load(null, null);
				for (Map.Entry<String, X509Certificate> entry : certificates.entrySet())
					keyStore.setCertificateEntry(entry.getKey(), entry.getValue());
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(keyStore, null);
				keyManagers = kmf.getKeyManagers();
			}

			if (trustAllCerts) {
				TrustManager[] trustAllManagers = new TrustManager[]{
						new X509TrustManager() {
							public void checkClientTrusted(X509Certificate[] chain, String authType) {
							}

							public void checkServerTrusted(X509Certificate[] chain, String authType) {
							}

							public X509Certificate[] getAcceptedIssuers() {
								return new X509Certificate[0];
							}
						}
				};
				sslContext.init(keyManagers, trustAllManagers, new SecureRandom());
			} else {
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore) null);
				sslContext.init(keyManagers, tmf.getTrustManagers(), new SecureRandom());
			}

			HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
			httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
			httpsConn.setHostnameVerifier((hostname, session) -> true);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException |
				 KeyManagementException | IOException e) {
			throw new RuntimeException("Failed to set up SSL context.", e);
		}
	}

	public Response execute() throws RequestException {
		configureSSLContext();
		if (this.body != null) {
			try (OutputStream os = connection.getOutputStream()) {
				os.write(body, 0, body.length);
				os.flush();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		try {
			connection.connect();
		} catch (IOException e) {
			throw new RequestException(e);
		}
		Response response = new Response(this);
		if (response.statusCode() < 200 || response.statusCode() > 399)
			throw new RequestException(response);
		return response;
	}
}
