package com.honeygroup.honeylms.teaching;

import com.honeygroup.honeylms.teaching.dto.EnrolledStudent;
import com.honeygroup.honeylms.teaching.dto.ManagedAssignmentSummary;
import com.honeygroup.honeylms.teaching.dto.ManagedCourseSummary;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Trainer workspace read endpoints. TRAINER or ADMIN only (see SecurityConfig). */
@RestController
public class TeachingController {

    private final TeachingService teachingService;

    public TeachingController(TeachingService teachingService) {
        this.teachingService = teachingService;
    }

    /** G5 — « Mes formations » (Trainer: assigned courses; Admin: all), DRAFT included. */
    @GetMapping("/api/me/managed-courses")
    public ResponseEntity<List<ManagedCourseSummary>> managedCourses(Authentication authentication) {
        return ResponseEntity.ok(teachingService.listManagedCourses(authentication.getName()));
    }

    /** G11 — « Corrections »: assignments of the managed courses with their counters. */
    @GetMapping("/api/me/managed-assignments")
    public ResponseEntity<List<ManagedAssignmentSummary>> managedAssignments(Authentication authentication) {
        return ResponseEntity.ok(teachingService.listManagedAssignments(authentication.getName()));
    }

    /** G10 — students enrolled in a course (to list who has not submitted). */
    @GetMapping("/api/courses/{courseId}/students")
    public ResponseEntity<List<EnrolledStudent>> students(@PathVariable Long courseId,
                                                          Authentication authentication) {
        return ResponseEntity.ok(teachingService.listStudents(courseId, authentication.getName()));
    }
}
