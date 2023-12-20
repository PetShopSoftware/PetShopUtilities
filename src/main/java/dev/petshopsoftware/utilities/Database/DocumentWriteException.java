package dev.petshopsoftware.utilities.Database;

public class DocumentWriteException extends RuntimeException{
    public DocumentWriteException(Exception exception) {
        super(exception);
    }
    public DocumentWriteException(String message) {
        super(message);
    }
}
