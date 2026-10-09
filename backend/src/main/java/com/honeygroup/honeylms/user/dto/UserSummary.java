package com.honeygroup.honeylms.user.dto;

import com.honeygroup.honeylms.user.UserAccount;
import java.time.Instant;

/**
 * Public view of an account (never the password hash). Used by the login answer, the Admin
 * user list and the trainers of a course. active / createdAt added for the Admin list (gap G9).
 */
public record UserSummary(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean active,
        Instant createdAt
) {

    /** Single mapping point: every service builds its UserSummary here. */
    public static UserSummary from(UserAccount account) {
        return new UserSummary(
                account.getId(),
                account.getEmail(),
                account.getFirstName(),
                account.getLastName(),
                account.getRole().getCode(),
                account.isActive(),
                account.getCreatedAt()
        );
    }
}
