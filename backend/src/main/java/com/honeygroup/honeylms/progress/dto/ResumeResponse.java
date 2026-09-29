package com.honeygroup.honeylms.progress.dto;

import java.time.LocalDateTime;

public record ResumeResponse(
        Long lessonId,
        Long moduleId,
        LocalDateTime lastViewedAt
) {
}
