package com.honeygroup.honeylms.course.dto;

/**
 * What a Student sees in the catalog - never exposes status (only PUBLISHED
 * courses reach this DTO at all) nor internal fields like createdBy.
 */
public record CourseSummary(
        Long id,
        String title,
        String description,
        String category
) {
}
