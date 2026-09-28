package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.CreateLessonRequest;
import com.honeygroup.honeylms.course.dto.LessonDetail;
import com.honeygroup.honeylms.course.dto.UpdateLessonRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints require authentication. Writes are TRAINER/ADMIN only; reads are
 * role-aware: Trainer/Admin see the management view, a Student needs Enrollment and
 * published Course/Module/Lesson (US-LEARNING-07).
 */
@RestController
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    /** US-LEARNING-04 — Create lesson. */
    @PostMapping("/api/modules/{moduleId}/lessons")
    public ResponseEntity<LessonDetail> createLesson(@PathVariable Long moduleId,
                                                       @Valid @RequestBody CreateLessonRequest request,
                                                       Authentication authentication) {
        LessonDetail created = lessonService.createLesson(moduleId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/modules/{moduleId}/lessons")
    public ResponseEntity<List<LessonDetail>> listLessons(@PathVariable Long moduleId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(lessonService.listLessons(moduleId, authentication.getName()));
    }

    @GetMapping("/api/lessons/{lessonId}")
    public ResponseEntity<LessonDetail> getLesson(@PathVariable Long lessonId, Authentication authentication) {
        return ResponseEntity.ok(lessonService.getLesson(lessonId, authentication.getName()));
    }

    /** US-LEARNING-05 — Update lesson. */
    @PutMapping("/api/lessons/{lessonId}")
    public ResponseEntity<LessonDetail> updateLesson(@PathVariable Long lessonId,
                                                       @Valid @RequestBody UpdateLessonRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(lessonService.updateLesson(lessonId, request, authentication.getName()));
    }

    /** US-LEARNING-06 — Publish lesson. */
    @PatchMapping("/api/lessons/{lessonId}/publish")
    public ResponseEntity<LessonDetail> publishLesson(@PathVariable Long lessonId, Authentication authentication) {
        return ResponseEntity.ok(lessonService.publishLesson(lessonId, authentication.getName()));
    }
}
