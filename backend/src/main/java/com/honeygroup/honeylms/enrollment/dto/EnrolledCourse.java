package com.honeygroup.honeylms.enrollment.dto;

import java.time.LocalDateTime;

public record EnrolledCourse(
        Long courseId,
        String title,
        String description,
        String category,
        LocalDateTime enrolledAt
) {
}
