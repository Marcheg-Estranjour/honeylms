package com.honeygroup.honeylms.submission.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * US-SUB-06/07. Both fields optional - grading stays optional per the
 * validated business rule, and a Trainer may leave feedback with no grade yet.
 * @DecimalMin/@DecimalMax only apply when grade is non-null.
 */
public record CorrectionRequest(
        @DecimalMin(value = "0", message = "Grade must be at least 0")
        @DecimalMax(value = "20", message = "Grade must be at most 20")
        BigDecimal grade,
        String feedback
) {
}
