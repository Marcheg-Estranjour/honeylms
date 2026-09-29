package com.honeygroup.honeylms.trainingclass;

import com.honeygroup.honeylms.trainingclass.dto.ClassMembersResponse;
import com.honeygroup.honeylms.trainingclass.dto.CreateClassRequest;
import com.honeygroup.honeylms.trainingclass.dto.TrainingClassDetail;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingClassController {

    private final TrainingClassService trainingClassService;

    public TrainingClassController(TrainingClassService trainingClassService) {
        this.trainingClassService = trainingClassService;
    }

    /** US-CLASS-01 — Create class. TRAINER/ADMIN only (see SecurityConfig). */
    @PostMapping("/api/courses/{courseId}/classes")
    public ResponseEntity<TrainingClassDetail> createClass(@PathVariable Long courseId,
                                                             @Valid @RequestBody CreateClassRequest request,
                                                             Authentication authentication) {
        TrainingClassDetail created = trainingClassService.createClass(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** US-CLASS-02 — Add student. */
    @PostMapping("/api/classes/{classId}/students/{studentId}")
    public ResponseEntity<Void> addStudent(@PathVariable Long classId, @PathVariable Long studentId,
                                            Authentication authentication) {
        trainingClassService.addStudent(classId, studentId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** US-CLASS-03 — Remove student. */
    @DeleteMapping("/api/classes/{classId}/students/{studentId}")
    public ResponseEntity<Void> removeStudent(@PathVariable Long classId, @PathVariable Long studentId,
                                               Authentication authentication) {
        trainingClassService.removeStudent(classId, studentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** US-CLASS-04 — Add trainer. */
    @PostMapping("/api/classes/{classId}/trainers/{trainerId}")
    public ResponseEntity<Void> addTrainer(@PathVariable Long classId, @PathVariable Long trainerId,
                                            Authentication authentication) {
        trainingClassService.addTrainer(classId, trainerId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Symmetric with removeStudent - see TrainingClassService javadoc. */
    @DeleteMapping("/api/classes/{classId}/trainers/{trainerId}")
    public ResponseEntity<Void> removeTrainer(@PathVariable Long classId, @PathVariable Long trainerId,
                                               Authentication authentication) {
        trainingClassService.removeTrainer(classId, trainerId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** US-CLASS-05 — View class members. */
    @GetMapping("/api/classes/{classId}/members")
    public ResponseEntity<ClassMembersResponse> getMembers(@PathVariable Long classId, Authentication authentication) {
        return ResponseEntity.ok(trainingClassService.getMembers(classId, authentication.getName()));
    }
}
