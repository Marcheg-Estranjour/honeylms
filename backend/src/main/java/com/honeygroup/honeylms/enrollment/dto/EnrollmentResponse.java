package com.honeygroup.honeylms.enrollment.dto;

import java.time.Instant;

public record EnrollmentResponse(
        Long courseId,
        Instant enrolledAt
) {
}
