package com.honeygroup.honeylms.user.dto;

public record UserSummary(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
