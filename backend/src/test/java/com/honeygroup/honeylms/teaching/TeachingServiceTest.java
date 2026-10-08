package com.honeygroup.honeylms.teaching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.common.IdCount;
import com.honeygroup.honeylms.course.Assignment;
import com.honeygroup.honeylms.course.AssignmentRepository;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.CourseCategory;
import com.honeygroup.honeylms.course.CourseModule;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.Lesson;
import com.honeygroup.honeylms.course.PublicationStatus;
import com.honeygroup.honeylms.enrollment.Enrollment;
import com.honeygroup.honeylms.enrollment.EnrollmentRepository;
import com.honeygroup.honeylms.submission.SubmissionRepository;
import com.honeygroup.honeylms.submission.SubmissionStatus;
import com.honeygroup.honeylms.teaching.dto.EnrolledStudent;
import com.honeygroup.honeylms.teaching.dto.ManagedAssignmentSummary;
import com.honeygroup.honeylms.teaching.dto.ManagedCourseSummary;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeachingServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @InjectMocks
    private TeachingService teachingService;

    private static final String TRAINER_EMAIL = "trainer@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";

    private UserAccount trainer() {
        return UserAccount.builder().id(2L).email(TRAINER_EMAIL).role(new Role(2L, "TRAINER", "Trainer")).build();
    }

    private UserAccount admin() {
        return UserAccount.builder().id(1L).email(ADMIN_EMAIL).role(new Role(3L, "ADMIN", "Admin")).build();
    }

    private Course course(Long id, String title, PublicationStatus status) {
        return Course.builder().id(id).title(title).description("desc")
                .category(CourseCategory.LANGUAGES).status(status).build();
    }

    private Assignment assignment(Long id, Course course, Instant dueDate) {
        CourseModule module = CourseModule.builder().id(id * 10).course(course).build();
        Lesson lesson = Lesson.builder().id(id * 100).title("Leçon " + id).courseModule(module).build();
        return Assignment.builder().id(id).title("Devoir " + id).lesson(lesson)
                .status(PublicationStatus.PUBLISHED).dueDate(dueDate).build();
    }

    // ---- listManagedCourses() ----

    @Test
    void listManagedCourses_trainerGetsAssignedCoursesWithCounters() {
        Course english = course(10L, "Anglais", PublicationStatus.PUBLISHED);
        Course italian = course(11L, "Italien", PublicationStatus.DRAFT);
        when(courseAuthorizationService.resolveRequester(TRAINER_EMAIL)).thenReturn(trainer());
        when(courseRepository.findManagedByTrainer(2L)).thenReturn(List.of(english, italian));
        when(enrollmentRepository.countByCourse(List.of(10L, 11L))).thenReturn(List.of(new IdCount(10L, 3L)));
        when(submissionRepository.countByCourseAndStatus(List.of(10L, 11L), SubmissionStatus.SUBMITTED))
                .thenReturn(List.of(new IdCount(10L, 2L)));

        List<ManagedCourseSummary> result = teachingService.listManagedCourses(TRAINER_EMAIL);

        assertThat(result).containsExactly(
                new ManagedCourseSummary(10L, "Anglais", "desc", "LANGUAGES", "PUBLISHED", 3L, 2L),
                new ManagedCourseSummary(11L, "Italien", "desc", "LANGUAGES", "DRAFT", 0L, 0L));
        verify(courseRepository, never()).findAllByOrderByTitleAsc();
    }

    @Test
    void listManagedCourses_adminGetsEveryCourse() {
        when(courseAuthorizationService.resolveRequester(ADMIN_EMAIL)).thenReturn(admin());
        when(courseRepository.findAllByOrderByTitleAsc())
                .thenReturn(List.of(course(10L, "Anglais", PublicationStatus.PUBLISHED)));
        when(enrollmentRepository.countByCourse(List.of(10L))).thenReturn(List.of());
        when(submissionRepository.countByCourseAndStatus(List.of(10L), SubmissionStatus.SUBMITTED))
                .thenReturn(List.of());

        assertThat(teachingService.listManagedCourses(ADMIN_EMAIL)).hasSize(1);
        verify(courseRepository, never()).findManagedByTrainer(any());
    }

    @Test
    void listManagedCourses_returnsEmptyWithoutQueryingCounters_whenNoCourse() {
        when(courseAuthorizationService.resolveRequester(TRAINER_EMAIL)).thenReturn(trainer());
        when(courseRepository.findManagedByTrainer(2L)).thenReturn(List.of());

        assertThat(teachingService.listManagedCourses(TRAINER_EMAIL)).isEmpty();
        verifyNoInteractions(enrollmentRepository, submissionRepository);
    }

    // ---- listManagedAssignments() ----

    @Test
    void listManagedAssignments_returnsCountersSortedByDeadline_noDeadlineLast() {
        Course english = course(10L, "Anglais", PublicationStatus.PUBLISHED);
        Instant soon = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant later = Instant.now().plus(14, ChronoUnit.DAYS);
        Assignment noDeadline = assignment(1L, english, null);
        Assignment lateOne = assignment(2L, english, later);
        Assignment soonOne = assignment(3L, english, soon);

        when(courseAuthorizationService.resolveRequester(TRAINER_EMAIL)).thenReturn(trainer());
        when(courseRepository.findManagedByTrainer(2L)).thenReturn(List.of(english));
        when(assignmentRepository.findByLesson_CourseModule_Course_IdIn(List.of(10L)))
                .thenReturn(List.of(noDeadline, lateOne, soonOne));
        when(enrollmentRepository.countByCourse(List.of(10L))).thenReturn(List.of(new IdCount(10L, 5L)));
        when(submissionRepository.countByAssignment(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(new IdCount(2L, 4L), new IdCount(3L, 1L)));
        when(submissionRepository.countByAssignmentAndStatus(List.of(1L, 2L, 3L), SubmissionStatus.SUBMITTED))
                .thenReturn(List.of(new IdCount(2L, 3L)));

        List<ManagedAssignmentSummary> result = teachingService.listManagedAssignments(TRAINER_EMAIL);

        assertThat(result).extracting(ManagedAssignmentSummary::id).containsExactly(3L, 2L, 1L);
        assertThat(result.get(1)).isEqualTo(new ManagedAssignmentSummary(
                2L, "Devoir 2", later, "PUBLISHED", 10L, "Anglais", 200L, "Leçon 2", 5L, 4L, 3L));
        assertThat(result.get(2).submissions()).isZero();
    }

    @Test
    void listManagedAssignments_returnsEmpty_whenCoursesHaveNoAssignment() {
        Course english = course(10L, "Anglais", PublicationStatus.PUBLISHED);
        when(courseAuthorizationService.resolveRequester(TRAINER_EMAIL)).thenReturn(trainer());
        when(courseRepository.findManagedByTrainer(2L)).thenReturn(List.of(english));
        when(assignmentRepository.findByLesson_CourseModule_Course_IdIn(List.of(10L))).thenReturn(List.of());

        assertThat(teachingService.listManagedAssignments(TRAINER_EMAIL)).isEmpty();
        verifyNoInteractions(submissionRepository);
    }

    // ---- listStudents() ----

    @Test
    void listStudents_returnsEnrolledStudents_whenAllowed() {
        Course english = course(10L, "Anglais", PublicationStatus.PUBLISHED);
        UserAccount trainer = trainer();
        UserAccount camille = UserAccount.builder().id(5L).firstName("Camille").lastName("Martin")
                .email("camille@example.com").build();
        Enrollment enrollment = Enrollment.of(camille, english);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(english));
        when(courseAuthorizationService.resolveRequester(TRAINER_EMAIL)).thenReturn(trainer);
        when(enrollmentRepository.findWithStudentByCourseId(10L)).thenReturn(List.of(enrollment));

        List<EnrolledStudent> result = teachingService.listStudents(10L, TRAINER_EMAIL);

        assertThat(result).containsExactly(new EnrolledStudent(
                5L, "Camille", "Martin", "camille@example.com", enrollment.getEnrolledAt()));
        verify(courseAuthorizationService).assertCanManageCourse(english, trainer);
    }

    @Test
    void listStudents_propagatesForbidden_whenTrainerNotAssigned() {
        Course english = course(10L, "Anglais", PublicationStatus.PUBLISHED);
        UserAccount trainer = trainer();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(english));
        when(courseAuthorizationService.resolveRequester(TRAINER_EMAIL)).thenReturn(trainer);
        doThrow(new ForbiddenActionException("nope"))
                .when(courseAuthorizationService).assertCanManageCourse(english, trainer);

        assertThatThrownBy(() -> teachingService.listStudents(10L, TRAINER_EMAIL))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void listStudents_throwsNotFound_whenCourseDoesNotExist() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teachingService.listStudents(99L, TRAINER_EMAIL))
                .isInstanceOf(CourseNotFoundException.class);
    }
}
