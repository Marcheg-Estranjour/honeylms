package com.honeygroup.honeylms.enrollment;

import com.honeygroup.honeylms.enrollment.dto.EnrolledCourse;
import com.honeygroup.honeylms.enrollment.dto.EnrollmentResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * STUDENT-only endpoints (enforced in SecurityConfig).
 */
@RestController
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /** US-ENROLL-01 — Enroll in course. */
    @PostMapping("/api/courses/{courseId}/enrollment")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long courseId, Authentication authentication) {
        EnrollmentResponse response = enrollmentService.enroll(courseId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** US-ENROLL-02 — View my courses. */
    @GetMapping("/api/me/courses")
    public ResponseEntity<List<EnrolledCourse>> myCourses(Authentication authentication) {
        return ResponseEntity.ok(enrollmentService.listMyCourses(authentication.getName()));
    }
}
