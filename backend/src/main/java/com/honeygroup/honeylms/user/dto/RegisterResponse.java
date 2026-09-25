package com.honeygroup.honeylms.user.dto;

/**
 * Response for POST /api/auth/register.
 * Never exposes the password hash - only what the client needs to confirm the account.
 */
public record RegisterResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
