package com.honeygroup.honeylms.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    @InjectMocks
    private AuditService auditService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void record_savesEventWithTheAuthenticatedUserAsActor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@honeylms.test", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        auditService.record(AuditAction.COURSE_CREATED, "COURSE", 42L, Map.of("title", "Excel"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.getAction()).isEqualTo(AuditAction.COURSE_CREATED);
        assertThat(event.getActorEmail()).isEqualTo("admin@honeylms.test");
        assertThat(event.getTargetType()).isEqualTo("COURSE");
        assertThat(event.getTargetId()).isEqualTo("42");
        assertThat(event.getDetails()).containsEntry("title", "Excel");
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void recordLogin_keepsTheAttemptedEmail_evenForAnUnknownAccount() {
        auditService.recordLogin(AuditAction.LOGIN_FAILURE, "unknown@example.com", null,
                Map.of("reason", "UNKNOWN_EMAIL"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActorEmail()).isEqualTo("unknown@example.com");
        assertThat(captor.getValue().getTargetId()).isNull();
    }

    @Test
    void recordLogin_neverBreaksTheBusinessAction_whenMongoIsDown() {
        when(repository.save(any(AuditEvent.class))).thenThrow(new RuntimeException("MongoDB unavailable"));

        assertThatCode(() -> auditService.recordLogin(AuditAction.LOGIN_SUCCESS, "alice@example.com", 1L, Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void search_capsThePageSizeAt100() {
        auditService.search(null, null, 0, 1000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByOccurredAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void search_filtersByNormalizedActorEmail() {
        auditService.search("  Admin@HoneyLMS.test ", null, 0, 20);

        verify(repository).findByActorEmailOrderByOccurredAtDesc(eq("admin@honeylms.test"), any(Pageable.class));
    }
}
