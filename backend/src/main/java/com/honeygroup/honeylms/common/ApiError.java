package com.honeygroup.honeylms.common;

import java.time.OffsetDateTime;

/**
 * Standard error envelope for every API error response
 * (see Dossier de Conception §12.3).
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path);
    }
}
