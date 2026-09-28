package com.honeygroup.honeylms.course.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * US-LEARNING-02. Reordering/status are not touched here - status has its
 * own explicit publish action (US-LEARNING-03), reordering is a COULD HAVE.
 */
public record UpdateModuleRequest(
        @NotBlank(message = "Title is required") String title,
        String description
) {
}
