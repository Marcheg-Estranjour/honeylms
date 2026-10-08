package com.honeygroup.honeylms.teaching.dto;

import java.time.Instant;

/** A student enrolled in a course — GET /api/courses/{id}/students (gap G10, « Non rendu »). */
public record EnrolledStudent(
        Long id,
        String firstName,
        String lastName,
        String email,
        Instant enrolledAt
) {
}
