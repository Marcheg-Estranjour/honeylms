package com.honeygroup.honeylms.audit;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * NoSQL data access (Spring Data MongoDB). As with the JPA repositories, queries are
 * derived from the method names: no query string is ever built by hand, so no
 * injection is possible.
 */
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    List<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    List<AuditEvent> findByActorEmailOrderByOccurredAtDesc(String actorEmail, Pageable pageable);

    List<AuditEvent> findByActionOrderByOccurredAtDesc(AuditAction action, Pageable pageable);
}
