package com.honeygroup.honeylms.audit.dto;

import com.honeygroup.honeylms.audit.AuditEvent;
import java.time.Instant;
import java.util.Map;

/** Read-only view of an audit entry returned by GET /api/admin/audit. */
public record AuditEventView(
        String id,
        String action,
        String actorEmail,
        String targetType,
        String targetId,
        Instant occurredAt,
        String ipAddress,
        Map<String, String> details
) {
    public static AuditEventView from(AuditEvent e) {
        return new AuditEventView(e.getId(), e.getAction().name(), e.getActorEmail(), e.getTargetType(),
                e.getTargetId(), e.getOccurredAt(), e.getIpAddress(), e.getDetails());
    }
}
