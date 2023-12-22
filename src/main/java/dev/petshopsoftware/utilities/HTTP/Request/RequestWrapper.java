package dev.petshopsoftware.utilities.HTTP.Request;

public abstract class RequestWrapper<T extends ResponseWrapper<?>> {
	protected abstract Request makeRequest();

	public T execute() throws RequestException {
		Response httpResponse = makeRequest().execute();
		return createResponse(httpResponse);
	}

	protected abstract T createResponse(Response response);
}
