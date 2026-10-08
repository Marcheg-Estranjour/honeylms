package com.honeygroup.honeylms.teaching.dto;

import java.time.Instant;

/**
 * An assignment of a course the requester manages, with its counters — one row of the
 * Trainer's « Corrections » screen (gap G11). DRAFT assignments are included.
 * notSubmitted = enrolledStudents - submissions (computed by the client).
 */
public record ManagedAssignmentSummary(
        Long id,
        String title,
        Instant dueDate,
        String status,
        Long courseId,
        String courseTitle,
        Long lessonId,
        String lessonTitle,
        long enrolledStudents,
        long submissions,
        long submissionsToCorrect
) {
}
