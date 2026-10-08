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
        long sizeBytes,
        /* « Camille Martin » — for the trainer's correction list (gap G10). */
        String studentName,
        /* Trainer who corrected it, null while not corrected (gap G10). */
        String correctedByName
) {
}
