package com.honeygroup.honeylms.enrollment.dto;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long courseId,
        LocalDateTime enrolledAt
) {
}
