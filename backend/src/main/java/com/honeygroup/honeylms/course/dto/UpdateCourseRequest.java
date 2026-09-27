package com.honeygroup.honeylms.course.dto;

import com.honeygroup.honeylms.course.CourseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * US-COURSE-04. Does not touch status - publishing/unpublishing stays a
 * separate action (US-COURSE-05) so it's always an explicit, auditable step.
 */
public record UpdateCourseRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Category is required") CourseCategory category
) {
}
