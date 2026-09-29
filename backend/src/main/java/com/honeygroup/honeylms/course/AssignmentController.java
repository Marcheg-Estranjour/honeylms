package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.AssignmentDetail;
import com.honeygroup.honeylms.course.dto.CreateAssignmentRequest;
import com.honeygroup.honeylms.course.dto.UpdateAssignmentRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /** US-ASSIGN-01 — Create assignment. TRAINER/ADMIN only. */
    @PostMapping("/api/lessons/{lessonId}/assignments")
    public ResponseEntity<AssignmentDetail> createAssignment(@PathVariable Long lessonId,
                                                               @Valid @RequestBody CreateAssignmentRequest request,
                                                               Authentication authentication) {
        AssignmentDetail created = assignmentService.createAssignment(lessonId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/lessons/{lessonId}/assignments")
    public ResponseEntity<List<AssignmentDetail>> listAssignments(@PathVariable Long lessonId,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(assignmentService.listAssignments(lessonId, authentication.getName()));
    }

    /** US-ASSIGN-04 — View assignment. Role-aware (see AssignmentService). */
    @GetMapping("/api/assignments/{assignmentId}")
    public ResponseEntity<AssignmentDetail> getAssignment(@PathVariable Long assignmentId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.getAssignment(assignmentId, authentication.getName()));
    }

    @PutMapping("/api/assignments/{assignmentId}")
    public ResponseEntity<AssignmentDetail> updateAssignment(@PathVariable Long assignmentId,
                                                               @Valid @RequestBody UpdateAssignmentRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(assignmentService.updateAssignment(assignmentId, request, authentication.getName()));
    }

    /** US-ASSIGN-02 — Publish assignment. */
    @PatchMapping("/api/assignments/{assignmentId}/publish")
    public ResponseEntity<AssignmentDetail> publishAssignment(@PathVariable Long assignmentId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(assignmentService.publishAssignment(assignmentId, authentication.getName()));
    }

    /** US-ASSIGN-03 — Attach files. TRAINER/ADMIN only. Can be called repeatedly. */
    @PostMapping(path = "/api/assignments/{assignmentId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentDetail> attachFile(@PathVariable Long assignmentId,
                                                         @RequestPart MultipartFile file,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.attachFile(assignmentId, file, authentication.getName()));
    }
}
