package com.honeygroup.honeylms.file;

/**
 * Thrown when an upload fails business validation (size, extension, MIME type,
 * empty file...). Mapped to 422 Unprocessable Entity - the request is well-formed
 * but the file itself is rejected.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
