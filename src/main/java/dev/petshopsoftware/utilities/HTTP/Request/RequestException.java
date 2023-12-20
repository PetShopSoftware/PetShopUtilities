package dev.petshopsoftware.utilities.HTTP.Request;

public class RequestException extends Exception {
    private final Response response;

    public RequestException(Response response) {
        super("Request failed with status " + response.statusCode() + ".");
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}
