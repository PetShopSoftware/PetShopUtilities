package dev.petshopsoftware.utilities.JWT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.petshopsoftware.utilities.JSON.JSON;

import java.util.Base64;

public class JWTHelper {
	public static JsonNode extractJWTToJSON(String jwt) throws JsonProcessingException {
		String[] parts = jwt.split("\\.");
		if (parts.length != 3) return null;
		String decodedPayload = new String(Base64.getUrlDecoder().decode(parts[1]));
		return JSON.MAPPER.readTree(decodedPayload);
	}
}
