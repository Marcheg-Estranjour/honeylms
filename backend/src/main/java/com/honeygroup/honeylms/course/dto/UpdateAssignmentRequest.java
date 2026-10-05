package com.honeygroup.honeylms.course.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record UpdateAssignmentRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        Instant dueDate
) {
}
