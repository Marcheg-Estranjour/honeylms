package com.honeygroup.honeylms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-ADMIN-06. Deliberately has NO role field: the TRAINER role is imposed by the
 * server, never sent by the client (otherwise this endpoint could create Admins).
 */
public record CreateTrainerRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName
) {
}
