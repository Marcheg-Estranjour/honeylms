package com.honeygroup.honeylms.course.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentDetail(
        Long id,
        Long lessonId,
        String title,
        String description,
        LocalDateTime dueDate,
        String status,
        List<AttachedFileSummary> files
) {
}
