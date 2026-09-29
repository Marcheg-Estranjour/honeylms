package com.honeygroup.honeylms.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.CourseCategory;
import com.honeygroup.honeylms.course.CourseModule;
import com.honeygroup.honeylms.course.CourseModuleRepository;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.LearningAccessService;
import com.honeygroup.honeylms.course.Lesson;
import com.honeygroup.honeylms.course.LessonRepository;
import com.honeygroup.honeylms.course.PublicationStatus;
import com.honeygroup.honeylms.progress.dto.CourseProgress;
import com.honeygroup.honeylms.progress.dto.LessonCompletionDetail;
import com.honeygroup.honeylms.progress.dto.ResumeResponse;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private LessonCompletionRepository completionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @Mock
    private LearningAccessService learningAccessService;

    @InjectMocks
    private ProgressService progressService;

    private UserAccount student() {
        return UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
    }

    private Lesson lesson(Long id) {
        Course course = Course.builder().id(10L).category(CourseCategory.LANGUAGES).status(PublicationStatus.PUBLISHED).build();
        CourseModule module = CourseModule.builder().id(100L).course(course).status(PublicationStatus.PUBLISHED).build();
        return Lesson.builder().id(id).courseModule(module).status(PublicationStatus.PUBLISHED).build();
    }

    @Test
    void markCompleted_createsRow_whenNoneExistsYet() {
        UserAccount student = student();
        Lesson lesson = lesson(1000L);

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(completionRepository.findByStudent_IdAndLesson_Id(5L, 1000L)).thenReturn(Optional.empty());
        when(completionRepository.save(any(LessonCompletion.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonCompletionDetail result = progressService.markCompleted(1000L, "student@example.com");

        assertThat(result.lessonId()).isEqualTo(1000L);
        assertThat(result.completedAt()).isNotNull();
        assertThat(result.lastViewedAt()).isNotNull();
    }

    @Test
    void markCompleted_updatesExistingRow_whenAlreadyViewed() {
        UserAccount student = student();
        Lesson lesson = lesson(1000L);
        LessonCompletion existing = LessonCompletion.newFor(student, lesson);
        existing.setLastViewedAt(LocalDateTime.now().minusDays(1));

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(completionRepository.findByStudent_IdAndLesson_Id(5L, 1000L)).thenReturn(Optional.of(existing));
        when(completionRepository.save(any(LessonCompletion.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonCompletionDetail result = progressService.markCompleted(1000L, "student@example.com");

        assertThat(result.completedAt()).isNotNull();
    }

    @Test
    void markCompleted_propagatesForbidden_whenNoAccess() {
        UserAccount student = student();
        Lesson lesson = lesson(1000L);

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        doThrow(new ForbiddenActionException("not enrolled"))
                .when(learningAccessService).assertStudentCanAccessLesson(lesson, student);

        assertThatThrownBy(() -> progressService.markCompleted(1000L, "student@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getCourseProgress_computesPercentage_fromAccessibleLessons() {
        UserAccount student = student();
        Course course = Course.builder().id(10L).status(PublicationStatus.PUBLISHED).build();
        List<Lesson> accessible = List.of(lesson(1L), lesson(2L), lesson(3L));

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(lessonRepository.findAccessibleLessonsByCourse(10L)).thenReturn(accessible);
        when(completionRepository.countByStudent_IdAndLesson_InAndCompletedAtIsNotNull(5L, accessible)).thenReturn(2L);

        CourseProgress result = progressService.getCourseProgress(10L, "student@example.com");

        assertThat(result.completedLessons()).isEqualTo(2L);
        assertThat(result.accessibleLessons()).isEqualTo(3L);
        assertThat(result.percentage()).isEqualTo(67); // round(2/3*100)
    }

    @Test
    void getCourseProgress_returnsZeroPercent_whenNoAccessibleLessons() {
        UserAccount student = student();
        Course course = Course.builder().id(10L).status(PublicationStatus.PUBLISHED).build();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(lessonRepository.findAccessibleLessonsByCourse(10L)).thenReturn(List.of());

        CourseProgress result = progressService.getCourseProgress(10L, "student@example.com");

        assertThat(result.percentage()).isZero();
        assertThat(result.accessibleLessons()).isZero();
    }

    @Test
    void getResumePoint_returnsLastViewedLesson() {
        UserAccount student = student();
        Course course = Course.builder().id(10L).status(PublicationStatus.PUBLISHED).build();
        Lesson lesson = lesson(1000L);
        LessonCompletion completion = LessonCompletion.newFor(student, lesson);
        completion.setLastViewedAt(LocalDateTime.now());

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(completionRepository.findByStudent_IdAndLesson_CourseModule_Course_IdOrderByLastViewedAtDesc(
                eq(5L), eq(10L), any())).thenReturn(List.of(completion));

        ResumeResponse result = progressService.getResumePoint(10L, "student@example.com");

        assertThat(result.lessonId()).isEqualTo(1000L);
        assertThat(result.moduleId()).isEqualTo(100L);
    }

    @Test
    void getResumePoint_throwsNoResumePoint_whenNothingViewedYet() {
        UserAccount student = student();
        Course course = Course.builder().id(10L).status(PublicationStatus.PUBLISHED).build();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(completionRepository.findByStudent_IdAndLesson_CourseModule_Course_IdOrderByLastViewedAtDesc(
                eq(5L), eq(10L), any())).thenReturn(List.of());

        assertThatThrownBy(() -> progressService.getResumePoint(10L, "student@example.com"))
                .isInstanceOf(NoResumePointException.class);
    }
}
