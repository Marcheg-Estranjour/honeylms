package com.honeygroup.honeylms.enrollment.dto;

import java.time.Instant;

public record EnrolledCourse(
        Long courseId,
        String title,
        String description,
        String category,
        Instant enrolledAt
) {
}
