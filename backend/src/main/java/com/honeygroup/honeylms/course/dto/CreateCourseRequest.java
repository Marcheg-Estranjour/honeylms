package com.honeygroup.honeylms.course.dto;

import com.honeygroup.honeylms.course.CourseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * US-COURSE-03. The Course is always created with status DRAFT -
 * publishing is a separate, explicit action (US-COURSE-05).
 */
public record CreateCourseRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Category is required") CourseCategory category
) {
}
