package com.honeygroup.honeylms.common;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One row of a « count ... group by id » JPQL query (constructor expression).
 * Avoids Object[] rows and keeps the aggregation in the database (no N+1).
 */
public record IdCount(Long id, Long count) {

    /** id -> count; ids without any row are absent (use getOrDefault(id, 0L)). */
    public static Map<Long, Long> toMap(Collection<IdCount> rows) {
        return rows.stream().collect(Collectors.toMap(IdCount::id, IdCount::count));
    }
}
