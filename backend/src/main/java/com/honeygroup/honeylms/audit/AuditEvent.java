package com.honeygroup.honeylms.audit;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One audit log entry, stored as a MongoDB document (collection audit_events).
 *
 * CHOIX TECHNIQUE : a document store fits an audit log - entries are only appended,
 * never joined with other data, and the "details" part varies with the action.
 * The TTL index on occurredAt makes MongoDB delete entries automatically after one
 * year (RGPD : IP addresses and emails are personal data, kept for a limited time).
 */
@Document(collection = "audit_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    private String id;

    @Indexed
    private AuditAction action;

    /** Email of the user who acted (or who tried to log in); null for an anonymous action. */
    @Indexed
    private String actorEmail;

    /** Kind of object concerned: USER_ACCOUNT, COURSE, SUBMISSION. */
    private String targetType;

    private String targetId;

    @Indexed(name = "occurred_at_ttl", expireAfter = "365d")
    private Instant occurredAt;

    private String ipAddress;

    /** Free-form data that depends on the action (title, grade, reason...). */
    private Map<String, String> details;
}
