package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.CourseDetail;
import com.honeygroup.honeylms.course.dto.CourseSummary;
import com.honeygroup.honeylms.course.dto.CreateCourseRequest;
import com.honeygroup.honeylms.course.dto.UpdateCourseRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * US-COURSE-01 — Browse courses. Public. Optional ?category= to filter the catalog.
     */
    @GetMapping
    public ResponseEntity<List<CourseSummary>> getCourses(
            @RequestParam(required = false) CourseCategory category) {
        return ResponseEntity.ok(courseService.getPublishedCourses(Optional.ofNullable(category)));
    }

    /**
     * US-COURSE-02 — View course. Public if PUBLISHED; DRAFT requires the owning
     * Trainer or an Admin (checked in the service, since anonymous access is
     * technically allowed to reach this endpoint - see SecurityConfig).
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetail> getCourse(@PathVariable Long courseId, Authentication authentication) {
        String requesterEmail = isRealUser(authentication) ? authentication.getName() : null;
        return ResponseEntity.ok(courseService.getCourseDetail(courseId, requesterEmail));
    }

    /**
     * US-COURSE-03 — Create course. TRAINER or ADMIN only (see SecurityConfig).
     */
    @PostMapping
    public ResponseEntity<CourseDetail> createCourse(@Valid @RequestBody CreateCourseRequest request,
                                                       Authentication authentication) {
        CourseDetail created = courseService.createCourse(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * US-COURSE-04 — Update course. Restricted to the Course's Trainer(s) or Admin
     * (checked in the service - it's data-dependent, not a plain role check).
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseDetail> updateCourse(@PathVariable Long courseId,
                                                       @Valid @RequestBody UpdateCourseRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request, authentication.getName()));
    }

    /**
     * US-COURSE-05 — Publish course. Same authorization rule as update.
     */
    @PatchMapping("/{courseId}/publish")
    public ResponseEntity<CourseDetail> publishCourse(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(courseService.publishCourse(courseId, authentication.getName()));
    }

    private boolean isRealUser(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
