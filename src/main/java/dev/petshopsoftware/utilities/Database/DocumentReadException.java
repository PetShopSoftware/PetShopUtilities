package dev.petshopsoftware.utilities.Database;

public class DocumentReadException extends Exception {
	public DocumentReadException(Exception exception) {
		super(exception);
	}

	public DocumentReadException(String message) {
		super(message);
	}
}
