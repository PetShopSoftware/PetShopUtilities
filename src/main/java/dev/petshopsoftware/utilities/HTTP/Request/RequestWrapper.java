package dev.petshopsoftware.utilities.HTTP.Request;

import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponseException;

public abstract class RequestWrapper<T extends ResponseWrapper<?>> {
	public abstract Request makeRequest();

	public T execute() throws HTTPResponseException {
		Response httpResponse;
		try {
			httpResponse = makeRequest().execute();
		} catch (RequestException e) {
			httpResponse = e.getResponse();
		}
		return createResponse(httpResponse);
	}

	public abstract T createResponse(Response response) throws HTTPResponseException;
}
