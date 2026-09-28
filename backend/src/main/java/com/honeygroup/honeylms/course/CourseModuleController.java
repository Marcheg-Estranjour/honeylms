package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.CreateModuleRequest;
import com.honeygroup.honeylms.course.dto.ModuleDetail;
import com.honeygroup.honeylms.course.dto.UpdateModuleRequest;
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
 * All endpoints here require authentication (see SecurityConfig: nothing under
 * /api/courses/{id}/modules or /api/modules/** is public) - this is a
 * Trainer/Admin management surface, not a Student-facing one (that's US-LEARNING-07).
 */
@RestController
public class CourseModuleController {

    private final CourseModuleService courseModuleService;

    public CourseModuleController(CourseModuleService courseModuleService) {
        this.courseModuleService = courseModuleService;
    }

    /** US-LEARNING-01 — Create module. */
    @PostMapping("/api/courses/{courseId}/modules")
    public ResponseEntity<ModuleDetail> createModule(@PathVariable Long courseId,
                                                       @Valid @RequestBody CreateModuleRequest request,
                                                       Authentication authentication) {
        ModuleDetail created = courseModuleService.createModule(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/courses/{courseId}/modules")
    public ResponseEntity<List<ModuleDetail>> listModules(@PathVariable Long courseId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(courseModuleService.listModules(courseId, authentication.getName()));
    }

    @GetMapping("/api/modules/{moduleId}")
    public ResponseEntity<ModuleDetail> getModule(@PathVariable Long moduleId, Authentication authentication) {
        return ResponseEntity.ok(courseModuleService.getModule(moduleId, authentication.getName()));
    }

    /** US-LEARNING-02 — Update module. */
    @PutMapping("/api/modules/{moduleId}")
    public ResponseEntity<ModuleDetail> updateModule(@PathVariable Long moduleId,
                                                       @Valid @RequestBody UpdateModuleRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(courseModuleService.updateModule(moduleId, request, authentication.getName()));
    }

    /** US-LEARNING-03 — Publish module. */
    @PatchMapping("/api/modules/{moduleId}/publish")
    public ResponseEntity<ModuleDetail> publishModule(@PathVariable Long moduleId, Authentication authentication) {
        return ResponseEntity.ok(courseModuleService.publishModule(moduleId, authentication.getName()));
    }
}
