package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.CourseDetail;
import com.honeygroup.honeylms.course.dto.CourseSummary;
import com.honeygroup.honeylms.course.dto.CreateCourseRequest;
import com.honeygroup.honeylms.course.dto.UpdateCourseRequest;
import com.honeygroup.honeylms.user.RoleCode;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseTrainerRepository courseTrainerRepository;
    private final UserAccountRepository userAccountRepository;

    public CourseService(CourseRepository courseRepository,
                          CourseTrainerRepository courseTrainerRepository,
                          UserAccountRepository userAccountRepository) {
        this.courseRepository = courseRepository;
        this.courseTrainerRepository = courseTrainerRepository;
        this.userAccountRepository = userAccountRepository;
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

        UserAccount requester = resolveRequester(requesterEmail);
        assertCanManageCourse(course, requester);
        return toDetail(course);
    }

    /**
     * US-COURSE-03 — Create course. TRAINER or ADMIN only (also enforced at
     * the URL level in SecurityConfig). Always created as DRAFT. The creating
     * Trainer is automatically added to CourseTrainer - an Admin creating a
     * course is not, since Admin already bypasses perimeter checks entirely.
     */
    @Transactional
    public CourseDetail createCourse(CreateCourseRequest request, String requesterEmail) {
        UserAccount requester = resolveRequester(requesterEmail);

        Course course = Course.builder()
                .title(request.title().trim())
                .description(request.description())
                .status(PublicationStatus.DRAFT)
                .category(request.category())
                .createdBy(requester)
                .build();

        Course saved = courseRepository.save(course);

        if (RoleCode.TRAINER.name().equals(requester.getRole().getCode())) {
            courseTrainerRepository.save(CourseTrainer.of(saved, requester));
        }

        return toDetail(saved);
    }

    /**
     * US-COURSE-04 — Update course. Restricted to the Trainer(s) assigned to
     * this Course, or Admin. Does not touch status.
     */
    @Transactional
    public CourseDetail updateCourse(Long courseId, UpdateCourseRequest request, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);
        UserAccount requester = resolveRequester(requesterEmail);
        assertCanManageCourse(course, requester);

        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setCategory(request.category());

        return toDetail(courseRepository.save(course));
    }

    /**
     * US-COURSE-05 — Publish course. Restricted to the Trainer(s) assigned to
     * this Course, or Admin. Idempotent: publishing an already-published Course
     * simply confirms its current state rather than failing.
     */
    @Transactional
    public CourseDetail publishCourse(Long courseId, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);
        UserAccount requester = resolveRequester(requesterEmail);
        assertCanManageCourse(course, requester);

        course.setStatus(PublicationStatus.PUBLISHED);
        return toDetail(courseRepository.save(course));
    }

    // ---- helpers ----

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private UserAccount resolveRequester(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in database: " + email));
    }

    /**
     * Central perimeter check for anything Course-scoped. ADMIN always passes;
     * a TRAINER must be explicitly assigned via CourseTrainer.
     */
    private void assertCanManageCourse(Course course, UserAccount requester) {
        if (RoleCode.ADMIN.name().equals(requester.getRole().getCode())) {
            return;
        }

        boolean isAssignedTrainer = courseTrainerRepository
                .existsByCourse_IdAndTrainer_Id(course.getId(), requester.getId());

        if (!isAssignedTrainer) {
            throw new ForbiddenActionException("You are not allowed to manage this course");
        }
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
