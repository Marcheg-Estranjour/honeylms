package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.CourseDetail;
import com.honeygroup.honeylms.course.dto.CourseSummary;
import com.honeygroup.honeylms.course.dto.CreateCourseRequest;
import com.honeygroup.honeylms.course.dto.UpdateCourseRequest;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseAuthorizationService courseAuthorizationService;

    public CourseService(CourseRepository courseRepository,
                          CourseAuthorizationService courseAuthorizationService) {
        this.courseRepository = courseRepository;
        this.courseAuthorizationService = courseAuthorizationService;
    }

    /**
     * US-COURSE-01 — Browse courses, grouped by domain (Langues/Bureautique/EDUCTOUR).
     * Only PUBLISHED courses are returned here.
     */
    @Transactional(readOnly = true)
    public List<CourseSummary> getPublishedCourses(Optional<CourseCategory> category) {
        List<Course> courses = category
                .map(c -> courseRepository.findByStatusAndCategory(PublicationStatus.PUBLISHED, c))
                .orElseGet(() -> courseRepository.findByStatus(PublicationStatus.PUBLISHED));

        return courses.stream().map(this::toSummary).toList();
    }

    /**
     * US-COURSE-02 — View course.
     * PUBLISHED courses are visible to anyone (requesterEmail may be null - anonymous).
     * DRAFT courses are only visible to ADMIN or a Trainer assigned to this Course.
     */
    @Transactional(readOnly = true)
    public CourseDetail getCourseDetail(Long courseId, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);

        if (course.getStatus() == PublicationStatus.PUBLISHED) {
            return toDetail(course);
        }

        if (requesterEmail == null) {
            throw new ForbiddenActionException("This course is not published yet");
        }

        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(course, requester);
        return toDetail(course);
    }

    /**
     * US-COURSE-03 — Create course. VALIDÉ : creating a course is reserved to the
     * Admin (a Trainer never creates a course - he manages the ones assigned to him
     * via CourseTrainer, see CourseTrainerService). Also enforced at URL level in
     * SecurityConfig; this check is the second barrier. Always created as DRAFT,
     * with no trainer assigned yet.
     */
    @Transactional
    public CourseDetail createCourse(CreateCourseRequest request, String requesterEmail) {
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertAdmin(requester);

        Course course = Course.builder()
                .title(request.title().trim())
                .description(request.description())
                .status(PublicationStatus.DRAFT)
                .category(request.category())
                .createdBy(requester)
                .build();

        return toDetail(courseRepository.save(course));
    }

    /**
     * US-COURSE-04 — Update course. Restricted to the Trainer(s) assigned to
     * this Course, or Admin. Does not touch status.
     */
    @Transactional
    public CourseDetail updateCourse(Long courseId, UpdateCourseRequest request, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(course, requester);

        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setCategory(request.category());

        return toDetail(courseRepository.save(course));
    }

    /**
     * US-COURSE-05 — Publish course. Restricted to the Trainer(s) assigned to
     * this Course, or Admin. Idempotent.
     */
    @Transactional
    public CourseDetail publishCourse(Long courseId, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(course, requester);

        course.setStatus(PublicationStatus.PUBLISHED);
        return toDetail(courseRepository.save(course));
    }

    /**
     * US-COURSE-06 — Unpublish course (VALIDÉ : a Trainer can publish AND unpublish
     * the courses assigned to him). Same authorization as publish. Existing
     * enrollments are kept, but Students lose access while the Course is DRAFT
     * (the access chain requires PUBLISHED), and progress is computed on what is
     * currently published (known risk, Dossier de Conception §19 risque 5).
     */
    @Transactional
    public CourseDetail unpublishCourse(Long courseId, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(course, requester);

        course.setStatus(PublicationStatus.DRAFT);
        return toDetail(courseRepository.save(course));
    }

    // ---- helpers ----

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private CourseSummary toSummary(Course course) {
        return new CourseSummary(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory().name()
        );
    }

    private CourseDetail toDetail(Course course) {
        return new CourseDetail(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getStatus().name(),
                course.getCategory().name(),
                course.getCreatedBy().getId()
        );
    }
}
