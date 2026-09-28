package com.honeygroup.honeylms.course.dto;

public record LessonDetail(
        Long id,
        Long moduleId,
        String title,
        String description,
        String content,
        Integer displayOrder,
        String status
) {
}
