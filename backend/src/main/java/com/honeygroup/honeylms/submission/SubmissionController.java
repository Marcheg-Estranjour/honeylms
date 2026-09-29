package com.honeygroup.honeylms.submission;

import com.honeygroup.honeylms.submission.dto.CorrectionRequest;
import com.honeygroup.honeylms.submission.dto.SubmissionDetail;
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
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** US-SUB-01 — Submit assignment. STUDENT only (see SecurityConfig). */
    @PostMapping(path = "/api/assignments/{assignmentId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionDetail> submit(@PathVariable Long assignmentId,
                                                     @RequestPart MultipartFile file,
                                                     Authentication authentication) {
        SubmissionDetail created = submissionService.submit(assignmentId, file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** US-SUB-02 — Replace submission. STUDENT (owner) only. */
    @PutMapping(path = "/api/submissions/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionDetail> replace(@PathVariable Long submissionId,
                                                      @RequestPart MultipartFile file,
                                                      Authentication authentication) {
        return ResponseEntity.ok(submissionService.replace(submissionId, file, authentication.getName()));
    }

    /** US-SUB-03 — View submission. Role-aware (owner Student, or Trainer/Admin of the perimeter). */
    @GetMapping("/api/submissions/{submissionId}")
    public ResponseEntity<SubmissionDetail> getSubmission(@PathVariable Long submissionId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(submissionService.getSubmission(submissionId, authentication.getName()));
    }

    /** US-SUB-04 — View submissions. TRAINER/ADMIN of the perimeter only. */
    @GetMapping("/api/assignments/{assignmentId}/submissions")
    public ResponseEntity<List<SubmissionDetail>> listSubmissions(@PathVariable Long assignmentId,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(submissionService.listSubmissions(assignmentId, authentication.getName()));
    }

    /** US-SUB-05/06/07 — Correct, grade and give feedback in one call. TRAINER/ADMIN only. */
    @PatchMapping("/api/submissions/{submissionId}/correction")
    public ResponseEntity<SubmissionDetail> correct(@PathVariable Long submissionId,
                                                      @Valid @RequestBody CorrectionRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(submissionService.correctSubmission(submissionId, request, authentication.getName()));
    }
}
