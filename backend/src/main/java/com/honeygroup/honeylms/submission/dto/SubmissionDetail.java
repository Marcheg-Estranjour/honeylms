package com.honeygroup.honeylms.submission.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmissionDetail(
        Long id,
        Long assignmentId,
        Long studentId,
        LocalDateTime submittedAt,
        String status,
        BigDecimal grade,
        String feedback,
        Long correctedByUserId,
        LocalDateTime correctedAt,
        String originalFileName,
        String mimeType,
        long sizeBytes
) {
}
