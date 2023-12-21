package dev.petshopsoftware.utilities.JWT;

import dev.petshopsoftware.utilities.Util.RandomUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class JWTFactory {
	private final String issuer;
	private final String secret;
	private final long expiration;
	private final Mac sha256HMAC;

	public JWTFactory(String issuer, String secret, long expiration) throws InvalidKeyException, NoSuchAlgorithmException {
		this.issuer = issuer;
		this.secret = secret;
		this.expiration = expiration;

		SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
		this.sha256HMAC = Mac.getInstance("HmacSHA256");
		this.sha256HMAC.init(secret_key);
	}

	public String makeJWT(JWTPayload jwtPayload) {
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

		String headerJson = new JWTHeader("HS256", "JWT").toJSONString(false);
		String header = encoder.encodeToString(headerJson.getBytes());

		String payloadJson = jwtPayload.toJSONString(false);
		String payload = encoder.encodeToString(payloadJson.getBytes());

		byte[] data = (header + "." + payload).getBytes();
		String signature = encoder.encodeToString(this.sha256HMAC.doFinal(data));

		return header + "." + payload + "." + signature;
	}

	public String makeJWT(String sub) {
		return makeJWT(makePayload(sub));
	}

	public JWTPayload makePayload(String sub) {
		long now = System.currentTimeMillis();
		return new JWTPayload(issuer, sub, now + expiration, now, RandomUtil.generateIdentifier());
	}

	public boolean validateJWTSignature(String jwt) {
		String[] parts = jwt.split("\\.");
		if (parts.length != 3) return false;

		String header = parts[0];
		String payload = parts[1];
		String signature = parts[2];

		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		byte[] data = (header + "." + payload).getBytes();
		String recomputedSignature = encoder.encodeToString(sha256HMAC.doFinal(data));
		return signature.equals(recomputedSignature);
	}

	public JWTPayload extractJWTPayload(String jwt) {
		String[] parts = jwt.split("\\.");
		if (parts.length != 3) return null;

		String payload = parts[1];
		Base64.Decoder decoder = Base64.getUrlDecoder();
		String payloadJson = new String(decoder.decode(payload));
		return (JWTPayload) new JWTPayload().fromString(payloadJson);
	}

	public boolean validate(String jwt, boolean allowExpired) {
		if (!validateJWTSignature(jwt)) return false;
		JWTPayload payload = extractJWTPayload(jwt);
		if (!allowExpired && payload.getExp() > System.currentTimeMillis()) return false;
		return payload.getIss().equals(issuer);
	}

	public boolean validate(String jwt) {
		return validate(jwt, false);
	}

	public String getIssuer() {
		return issuer;
	}

	public String getSecret() {
		return secret;
	}

	public long getExpiration() {
		return expiration;
	}

	public Mac getSha256HMAC() {
		return sha256HMAC;
	}
}
