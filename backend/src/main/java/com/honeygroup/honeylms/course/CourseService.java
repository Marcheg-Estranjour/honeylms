package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.CourseSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * US-COURSE-01 — Browse courses, grouped by domain (Langues/Bureautique/EDUCTOUR).
     * Only PUBLISHED courses are returned here - DRAFT courses are only visible
     * to their Trainer/Admin, which will be a separate, authenticated endpoint (US-COURSE-02+).
     */
    @Transactional(readOnly = true)
    public List<CourseSummary> getPublishedCourses(Optional<CourseCategory> category) {
        List<Course> courses = category
                .map(c -> courseRepository.findByStatusAndCategory(PublicationStatus.PUBLISHED, c))
                .orElseGet(() -> courseRepository.findByStatus(PublicationStatus.PUBLISHED));

        return courses.stream()
                .map(course -> new CourseSummary(
                        course.getId(),
                        course.getTitle(),
                        course.getDescription(),
                        course.getCategory().name()
                ))
                .toList();
    }
}
