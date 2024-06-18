package dev.petshopsoftware.utilities.HTTP.Request;

import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponse;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponseException;

public class RequestUtils {
	public static HTTPResponse handleDefaultResponses(Response response) {
		HTTPResponse httpResponse;
		switch (response.statusCode()) {
			case 400:
				httpResponse = HTTPResponse.BAD_REQUEST;
				break;
			case 404:
				httpResponse = HTTPResponse.NOT_FOUND;
				break;
			case 500:
				httpResponse = HTTPResponse.INTERNAL_ERROR;
				break;
			default:
				httpResponse = null;
		}
		if (httpResponse != null && response.jsonBody().path("message").isTextual())
			httpResponse = httpResponse.message(response.jsonBody().path("message").asText());
		return httpResponse;
	}

	public static void throwDefaultResponses(Response response) throws HTTPResponseException {
		HTTPResponse httpResponse = handleDefaultResponses(response);
		if (httpResponse != null)
			throw new HTTPResponseException(httpResponse);
	}


}
