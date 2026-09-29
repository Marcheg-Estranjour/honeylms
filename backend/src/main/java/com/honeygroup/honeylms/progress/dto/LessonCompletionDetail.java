package com.honeygroup.honeylms.progress.dto;

import java.time.LocalDateTime;

public record LessonCompletionDetail(
        Long lessonId,
        LocalDateTime lastViewedAt,
        LocalDateTime completedAt
) {
}
