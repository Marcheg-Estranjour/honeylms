package com.honeygroup.honeylms.user;

/**
 * Thrown when a registration is attempted with an email that is already in use.
 * Mapped to HTTP 409 Conflict by GlobalExceptionHandler.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account already exists with email: " + email);
    }
}
