package dev.petshopsoftware.utilities.HTTP.Request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.petshopsoftware.utilities.JSON.JSON;

public abstract class ResponseWrapper<T extends RequestWrapper<?>> implements JSON {
	@JsonIgnore
	protected final Response response;

	public ResponseWrapper(Response response) {
		this.response = response;
	}
}
