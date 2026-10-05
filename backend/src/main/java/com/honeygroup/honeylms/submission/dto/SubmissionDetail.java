package com.honeygroup.honeylms.submission.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SubmissionDetail(
        Long id,
        Long assignmentId,
        Long studentId,
        Instant submittedAt,
        String status,
        BigDecimal grade,
        String feedback,
        Long correctedByUserId,
        Instant correctedAt,
        String originalFileName,
        String mimeType,
        long sizeBytes
) {
}
