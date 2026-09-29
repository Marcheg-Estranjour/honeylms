package com.honeygroup.honeylms.progress.dto;

public record CourseProgress(
        Long courseId,
        long completedLessons,
        long accessibleLessons,
        int percentage
) {
}
