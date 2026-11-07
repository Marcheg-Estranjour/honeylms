package com.honeygroup.honeylms.audit;

/**
 * Sensitive actions recorded in the audit log (MongoDB, collection audit_events).
 * Added after the internship, following the security watch (OWASP A09 -
 * Security Logging and Monitoring Failures).
 */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGIN_REJECTED_DISABLED,
    TRAINER_CREATED,
    ACCOUNT_ACTIVATED,
    ACCOUNT_DEACTIVATED,
    COURSE_CREATED,
    TRAINER_ASSIGNED,
    TRAINER_UNASSIGNED,
    SUBMISSION_CORRECTED
}
