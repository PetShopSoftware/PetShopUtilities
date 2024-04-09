package dev.petshopsoftware.utilities.Database;

import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponse;
import dev.petshopsoftware.utilities.HTTP.Server.HTTPResponseException;

public class DocumentWriteException extends HTTPResponseException {
	public DocumentWriteException(String message) {
		super(HTTPResponse.INTERNAL_ERROR.message(message));
	}
}
