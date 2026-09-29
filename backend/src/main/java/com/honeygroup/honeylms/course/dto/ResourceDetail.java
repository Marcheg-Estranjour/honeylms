package com.honeygroup.honeylms.course.dto;

public record ResourceDetail(
        Long id,
        Long lessonId,
        String title,
        Integer displayOrder,
        String originalFileName,
        String mimeType,
        long sizeBytes
) {
}
