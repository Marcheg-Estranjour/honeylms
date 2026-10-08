package com.honeygroup.honeylms.teaching;

import com.honeygroup.honeylms.common.IdCount;
import com.honeygroup.honeylms.course.Assignment;
import com.honeygroup.honeylms.course.AssignmentRepository;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.Lesson;
import com.honeygroup.honeylms.enrollment.EnrollmentRepository;
import com.honeygroup.honeylms.submission.SubmissionRepository;
import com.honeygroup.honeylms.submission.SubmissionStatus;
import com.honeygroup.honeylms.teaching.dto.EnrolledStudent;
import com.honeygroup.honeylms.teaching.dto.ManagedAssignmentSummary;
import com.honeygroup.honeylms.teaching.dto.ManagedCourseSummary;
import com.honeygroup.honeylms.user.RoleCode;
import com.honeygroup.honeylms.user.UserAccount;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the Trainer workspace (« Mes formations », « Corrections ») — gaps G5, G10, G11.
 *
 * CHOIX TECHNIQUE : a separate « teaching » module that only READS from course, enrollment and
 * submission. The course module must not depend on enrollment (see EnrollmentChecker), so these
 * cross-module views cannot live in CourseService. Counters are computed with « group by »
 * queries: one query per counter, whatever the number of courses (no N+1 on the client).
 *
 * Scope: an Admin sees every course; a Trainer sees only the courses he is assigned to
 * (CourseTrainer). The URL rules (SecurityConfig) already exclude Students.
 */
@Service
public class TeachingService {

    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseAuthorizationService courseAuthorizationService;

    public TeachingService(CourseRepository courseRepository,
                           AssignmentRepository assignmentRepository,
                           EnrollmentRepository enrollmentRepository,
                           SubmissionRepository submissionRepository,
                           CourseAuthorizationService courseAuthorizationService) {
        this.courseRepository = courseRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.submissionRepository = submissionRepository;
        this.courseAuthorizationService = courseAuthorizationService;
    }

    /** G5 — courses the requester manages, DRAFT included, sorted by title. */
    @Transactional(readOnly = true)
    public List<ManagedCourseSummary> listManagedCourses(String requesterEmail) {
        List<Course> courses = managedCourses(courseAuthorizationService.resolveRequester(requesterEmail));
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, Long> enrolled = IdCount.toMap(enrollmentRepository.countByCourse(courseIds));
        Map<Long, Long> toCorrect = IdCount.toMap(
                submissionRepository.countByCourseAndStatus(courseIds, SubmissionStatus.SUBMITTED));

        return courses.stream()
                .map(course -> new ManagedCourseSummary(
                        course.getId(),
                        course.getTitle(),
                        course.getDescription(),
                        course.getCategory().name(),
                        course.getStatus().name(),
                        enrolled.getOrDefault(course.getId(), 0L),
                        toCorrect.getOrDefault(course.getId(), 0L)))
                .toList();
    }

    /**
     * G11 — assignments of the managed courses with their counters, nearest deadline first
     * (no deadline last), then by course title.
     */
    @Transactional(readOnly = true)
    public List<ManagedAssignmentSummary> listManagedAssignments(String requesterEmail) {
        List<Course> courses = managedCourses(courseAuthorizationService.resolveRequester(requesterEmail));
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<Assignment> assignments = assignmentRepository.findByLesson_CourseModule_Course_IdIn(courseIds);
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        Map<Long, Long> enrolled = IdCount.toMap(enrollmentRepository.countByCourse(courseIds));
        Map<Long, Long> submitted = IdCount.toMap(submissionRepository.countByAssignment(assignmentIds));
        Map<Long, Long> toCorrect = IdCount.toMap(
                submissionRepository.countByAssignmentAndStatus(assignmentIds, SubmissionStatus.SUBMITTED));

        return assignments.stream()
                .map(assignment -> {
                    Lesson lesson = assignment.getLesson();
                    Course course = lesson.getCourseModule().getCourse();
                    return new ManagedAssignmentSummary(
                            assignment.getId(),
                            assignment.getTitle(),
                            assignment.getDueDate(),
                            assignment.getStatus().name(),
                            course.getId(),
                            course.getTitle(),
                            lesson.getId(),
                            lesson.getTitle(),
                            enrolled.getOrDefault(course.getId(), 0L),
                            submitted.getOrDefault(assignment.getId(), 0L),
                            toCorrect.getOrDefault(assignment.getId(), 0L));
                })
                .sorted(Comparator
                        .comparing(ManagedAssignmentSummary::dueDate,
                                Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
                        .thenComparing(ManagedAssignmentSummary::courseTitle,
                                Comparator.nullsLast(Comparator.<String>naturalOrder())))
                .toList();
    }

    /**
     * G10 — students enrolled in a course, sorted by name. Needed to show who has NOT submitted.
     * Restricted to the course's Trainer(s) or an Admin (data-dependent, checked here).
     */
    @Transactional(readOnly = true)
    public List<EnrolledStudent> listStudents(Long courseId, String requesterEmail) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(course, requester);

        return enrollmentRepository.findWithStudentByCourseId(courseId).stream()
                .map(enrollment -> new EnrolledStudent(
                        enrollment.getStudent().getId(),
                        enrollment.getStudent().getFirstName(),
                        enrollment.getStudent().getLastName(),
                        enrollment.getStudent().getEmail(),
                        enrollment.getEnrolledAt()))
                .toList();
    }

    private List<Course> managedCourses(UserAccount requester) {
        if (RoleCode.ADMIN.name().equals(requester.getRole().getCode())) {
            return courseRepository.findAllByOrderByTitleAsc();
        }
        return courseRepository.findManagedByTrainer(requester.getId());
    }
}
