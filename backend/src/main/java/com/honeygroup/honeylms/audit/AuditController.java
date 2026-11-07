package com.honeygroup.honeylms.audit;

import com.honeygroup.honeylms.audit.dto.AuditEventView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Audit log consultation. ADMIN only (see SecurityConfig). */
@RestController
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/api/admin/audit")
    public ResponseEntity<List<AuditEventView>> search(@RequestParam(required = false) String actor,
                                                       @RequestParam(required = false) AuditAction action,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.search(actor, action, page, size));
    }
}
