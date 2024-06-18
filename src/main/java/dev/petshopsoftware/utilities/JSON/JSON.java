package dev.petshopsoftware.utilities.JSON;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface JSON {
	ObjectMapper MAPPER = new ObjectMapper()
			.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
			.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
			.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
			.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.NONE)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	static <T> T convert(JsonNode object) {
		return MAPPER.convertValue(object, new TypeReference<T>() {
		});
	}

	default JsonNode toJSON() {
		try {
			return MAPPER.readTree(MAPPER.writeValueAsString(this));
		} catch (JsonProcessingException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	default String toJSONString(boolean pretty) {
		if (pretty) return toJSON().toPrettyString();
		else return toJSON().toString();
	}

	default String toJSONString() {
		return toJSONString(true);
	}

	default <T extends JSON> T fromJSON(JsonNode json) {
		try {
			return MAPPER
					.readerForUpdating(this)
					.readValue(json);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	default <T extends JSON> T fromString(String string) {
		try {
			return fromJSON(MAPPER.readTree(string));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
