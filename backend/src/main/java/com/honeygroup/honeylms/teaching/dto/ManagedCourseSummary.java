package com.honeygroup.honeylms.teaching.dto;

/**
 * A course in the Trainer's « Mes formations » list or the Admin back-office (gap G5).
 * Unlike CourseSummary (public catalog), DRAFT courses are included and status is given.
 */
public record ManagedCourseSummary(
        Long id,
        String title,
        String description,
        String category,
        String status,
        long enrolledStudents,
        /* Submissions with status SUBMITTED (waiting for a correction). */
        long submissionsToCorrect
) {
}
