package com.honeygroup.honeylms.user;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("This account is disabled");
    }
}
