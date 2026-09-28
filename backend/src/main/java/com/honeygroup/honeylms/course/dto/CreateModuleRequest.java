package com.honeygroup.honeylms.course.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * US-LEARNING-01. No displayOrder field: it is always server-computed
 * (appended at the end) - never trusted from the client, to avoid
 * unique-constraint conflicts or arbitrary reordering.
 */
public record CreateModuleRequest(
        @NotBlank(message = "Title is required") String title,
        String description
) {
}
