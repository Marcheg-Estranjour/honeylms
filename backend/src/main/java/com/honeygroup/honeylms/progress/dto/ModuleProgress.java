package com.honeygroup.honeylms.progress.dto;

public record ModuleProgress(
        Long moduleId,
        long completedLessons,
        long accessibleLessons,
        int percentage
) {
}
