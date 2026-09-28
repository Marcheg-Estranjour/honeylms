package com.honeygroup.honeylms.course.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLessonRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        String content
) {
}
