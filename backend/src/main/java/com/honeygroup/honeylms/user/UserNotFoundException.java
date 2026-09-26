package com.honeygroup.honeylms.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("No user account found with id: " + userId);
    }
}
