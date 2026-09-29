package com.honeygroup.honeylms.course.dto;

public record AttachedFileSummary(
        Long storedFileId,
        String originalName,
        String mimeType,
        long sizeBytes
) {
}
