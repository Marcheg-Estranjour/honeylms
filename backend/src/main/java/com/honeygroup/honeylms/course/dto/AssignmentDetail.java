package com.honeygroup.honeylms.course.dto;

import java.time.Instant;
import java.util.List;

public record AssignmentDetail(
        Long id,
        Long lessonId,
        String title,
        String description,
        Instant dueDate,
        String status,
        List<AttachedFileSummary> files
) {
}
