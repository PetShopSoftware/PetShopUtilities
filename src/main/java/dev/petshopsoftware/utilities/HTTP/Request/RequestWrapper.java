package dev.petshopsoftware.utilities.HTTP.Request;

public abstract class RequestWrapper<T extends ResponseWrapper<?>> {
	public abstract Request makeRequest();

	public T execute() {
		Response httpResponse;
		try {
			httpResponse = makeRequest().execute();
		} catch (RequestException e) {
			httpResponse = e.getResponse();
		}
		return createResponse(httpResponse);
	}

	public abstract T createResponse(Response response);
}
