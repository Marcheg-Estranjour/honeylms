package com.honeygroup.honeylms.progress;

import com.honeygroup.honeylms.progress.dto.CourseCompletions;
import com.honeygroup.honeylms.progress.dto.CourseProgress;
import com.honeygroup.honeylms.progress.dto.LessonCompletionDetail;
import com.honeygroup.honeylms.progress.dto.ModuleProgress;
import com.honeygroup.honeylms.progress.dto.ResumeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** All endpoints here are STUDENT-only (see SecurityConfig). */
@RestController
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PutMapping("/api/lessons/{lessonId}/view")
    public ResponseEntity<LessonCompletionDetail> recordView(@PathVariable Long lessonId,
                                                               Authentication authentication) {
        return ResponseEntity.ok(progressService.recordView(lessonId, authentication.getName()));
    }

    /** US-PROGRESS-01 — Complete lesson. */
    @PostMapping("/api/lessons/{lessonId}/completion")
    public ResponseEntity<LessonCompletionDetail> markCompleted(@PathVariable Long lessonId,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(progressService.markCompleted(lessonId, authentication.getName()));
    }

    /** US-PROGRESS-02 — View course progress. */
    @GetMapping("/api/courses/{courseId}/progress")
    public ResponseEntity<CourseProgress> courseProgress(@PathVariable Long courseId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(progressService.getCourseProgress(courseId, authentication.getName()));
    }

    /** US-PROGRESS-03 — View module progress. */
    @GetMapping("/api/modules/{moduleId}/progress")
    public ResponseEntity<ModuleProgress> moduleProgress(@PathVariable Long moduleId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(progressService.getModuleProgress(moduleId, authentication.getName()));
    }

    /** Gap G1 — completed lessons of the course (check marks of the lesson view). */
    @GetMapping("/api/courses/{courseId}/completions")
    public ResponseEntity<CourseCompletions> courseCompletions(@PathVariable Long courseId,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(progressService.getCourseCompletions(courseId, authentication.getName()));
    }

    /** US-PROGRESS-04 — Resume learning. */
    @GetMapping("/api/courses/{courseId}/resume")
    public ResponseEntity<ResumeResponse> resume(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(progressService.getResumePoint(courseId, authentication.getName()));
    }
}
