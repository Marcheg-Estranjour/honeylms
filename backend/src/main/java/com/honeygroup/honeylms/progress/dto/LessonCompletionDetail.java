package com.honeygroup.honeylms.progress.dto;

import java.time.Instant;

public record LessonCompletionDetail(
        Long lessonId,
        Instant lastViewedAt,
        Instant completedAt
) {
}
