package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.CourseSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
     * US-COURSE-01 — Browse courses. Public endpoint (see SecurityConfig).
     * Optional ?category=LANGUAGES|OFFICE_AUTOMATION|EDUCTOUR to filter the catalog.
     */
    @GetMapping
    public ResponseEntity<List<CourseSummary>> getCourses(
            @RequestParam(required = false) CourseCategory category) {
        return ResponseEntity.ok(courseService.getPublishedCourses(Optional.ofNullable(category)));
    }
}
