package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.user.dto.UserSummary;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN only - see SecurityConfig. */
@RestController
public class CourseTrainerController {

    private final CourseTrainerService courseTrainerService;

    public CourseTrainerController(CourseTrainerService courseTrainerService) {
        this.courseTrainerService = courseTrainerService;
    }

    /** US-ADMIN-07 — Assign a Trainer to a Course. */
    @PostMapping("/api/courses/{courseId}/trainers/{trainerId}")
    public ResponseEntity<Void> assignTrainer(@PathVariable Long courseId, @PathVariable Long trainerId,
                                               Authentication authentication) {
        courseTrainerService.assignTrainer(courseId, trainerId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/courses/{courseId}/trainers/{trainerId}")
    public ResponseEntity<Void> unassignTrainer(@PathVariable Long courseId, @PathVariable Long trainerId,
                                                 Authentication authentication) {
        courseTrainerService.unassignTrainer(courseId, trainerId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/courses/{courseId}/trainers")
    public ResponseEntity<List<UserSummary>> listTrainers(@PathVariable Long courseId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(courseTrainerService.listTrainers(courseId, authentication.getName()));
    }
}
