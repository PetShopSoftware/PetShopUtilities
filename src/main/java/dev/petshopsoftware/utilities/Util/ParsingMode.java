package dev.petshopsoftware.utilities.Util;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.function.Function;

public enum ParsingMode {
	RAW((value) -> value),
	STRING(String::valueOf),
	JSON((value) -> {
		try {
			return dev.petshopsoftware.utilities.JSON.JSON.MAPPER.readTree(value.toString());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}),
	;

	public final Function<Object, ?> parse;

	<T> ParsingMode(Function<Object, T> parse) {
		this.parse = parse;
	}
}
