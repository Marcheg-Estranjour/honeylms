package com.honeygroup.honeylms.user;

/**
 * Thrown when an email doesn't exist OR the password doesn't match.
 * Deliberately the same exception/message for both cases : never reveal
 * whether it was the email or the password that was wrong.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
