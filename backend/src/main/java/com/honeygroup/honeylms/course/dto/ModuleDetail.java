package com.honeygroup.honeylms.course.dto;

public record ModuleDetail(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer displayOrder,
        String status
) {
}
