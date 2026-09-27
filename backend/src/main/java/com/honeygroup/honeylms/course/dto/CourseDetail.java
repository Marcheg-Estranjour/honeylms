package com.honeygroup.honeylms.course.dto;

public record CourseDetail(
        Long id,
        String title,
        String description,
        String status,
        String category,
        Long createdByUserId
) {
}
