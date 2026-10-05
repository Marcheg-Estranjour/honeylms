package com.honeygroup.honeylms.progress.dto;

import java.time.Instant;

public record ResumeResponse(
        Long lessonId,
        Long moduleId,
        Instant lastViewedAt
) {
}
