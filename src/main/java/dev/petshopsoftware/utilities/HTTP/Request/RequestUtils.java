package dev.petshopsoftware.utilities.HTTP.Request;

import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponse;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponseException;

public class RequestUtils {
	public static HTTPResponse handleDefaultResponses(Response response) {
		HTTPResponse httpResponse = switch (response.statusCode()) {
			case 400 -> HTTPResponse.BAD_REQUEST;
			case 404 -> HTTPResponse.NOT_FOUND;
			case 500 -> HTTPResponse.INTERNAL_ERROR;
			default -> null;
		};
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
