package com.honeygroup.honeylms.audit;

import com.honeygroup.honeylms.audit.dto.AuditEventView;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records sensitive actions in the audit log (MongoDB).
 *
 * Two rules:
 * - the audit log must never break a business action: if MongoDB is unavailable,
 *   the error is logged and the action goes on (best effort);
 * - an action is recorded only once it really happened: inside a database
 *   transaction, the entry is written after the commit (a rolled-back action
 *   leaves no trace). Login attempts are recorded immediately, failures included.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** Records an action made by the authenticated user (taken from the security context). */
    public void record(AuditAction action, String targetType, Object targetId, Map<String, String> details) {
        AuditEvent event = build(action, currentActor(), targetType, targetId, details);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    save(event);
                }
            });
        } else {
            save(event);
        }
    }

    /** Records a login attempt right away (the user is not authenticated yet). */
    public void recordLogin(AuditAction action, String email, Object accountId, Map<String, String> details) {
        save(build(action, email, "USER_ACCOUNT", accountId, details));
    }

    /** Latest entries, optionally filtered by actor or by action. Page size capped at 100. */
    public List<AuditEventView> search(String actorEmail, AuditAction action, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        List<AuditEvent> events;
        if (actorEmail != null && !actorEmail.isBlank()) {
            events = repository.findByActorEmailOrderByOccurredAtDesc(actorEmail.trim().toLowerCase(), pageable);
        } else if (action != null) {
            events = repository.findByActionOrderByOccurredAtDesc(action, pageable);
        } else {
            events = repository.findAllByOrderByOccurredAtDesc(pageable);
        }
        return events.stream().map(AuditEventView::from).toList();
    }

    // ---- helpers ----

    private AuditEvent build(AuditAction action, String actorEmail, String targetType, Object targetId,
                             Map<String, String> details) {
        return AuditEvent.builder()
                .action(action)
                .actorEmail(actorEmail)
                .targetType(targetType)
                .targetId(targetId != null ? String.valueOf(targetId) : null)
                .occurredAt(Instant.now())
                .ipAddress(currentIpAddress())
                .details(details != null ? details : Map.of())
                .build();
    }

    private void save(AuditEvent event) {
        try {
            repository.save(event);
        } catch (RuntimeException e) {
            log.warn("Audit event {} could not be saved: {}", event.getAction(), e.getMessage());
        }
    }

    private static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    /** Client IP: X-Real-IP set by nginx when present, otherwise the direct remote address. */
    private static String currentIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp : request.getRemoteAddr();
    }
}
