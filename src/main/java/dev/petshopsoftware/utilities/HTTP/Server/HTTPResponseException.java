package dev.petshopsoftware.utilities.HTTP.Server;

public class HTTPResponseException extends Exception {
	private final HTTPResponse response;

	public HTTPResponseException(HTTPResponse response) {
		this.response = response;
	}

	public HTTPResponse getResponse() {
		return response;
	}
}
