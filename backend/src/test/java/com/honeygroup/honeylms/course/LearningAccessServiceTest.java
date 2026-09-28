package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningAccessServiceTest {

    @Mock
    private EnrollmentChecker enrollmentChecker;

    @InjectMocks
    private LearningAccessService learningAccessService;

    private UserAccount student() {
        return UserAccount.builder().id(5L).role(new Role(1L, "STUDENT", "Student")).build();
    }

    private Lesson lesson(PublicationStatus course, PublicationStatus module, PublicationStatus lesson) {
        Course c = Course.builder().id(10L).status(course).build();
        CourseModule m = CourseModule.builder().id(100L).course(c).status(module).build();
        return Lesson.builder().id(1000L).courseModule(m).status(lesson).build();
    }

    @Test
    void isStudent_trueOnlyForStudentRole() {
        UserAccount trainer = UserAccount.builder().id(1L).role(new Role(2L, "TRAINER", "Trainer")).build();

        assertThat(learningAccessService.isStudent(student())).isTrue();
        assertThat(learningAccessService.isStudent(trainer)).isFalse();
    }

    @Test
    void assertStudentCanAccessLesson_passes_whenEnrolledAndEverythingPublished() {
        when(enrollmentChecker.isStudentEnrolled(5L, 10L)).thenReturn(true);
        Lesson l = lesson(PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);

        learningAccessService.assertStudentCanAccessLesson(l, student()); // no exception
    }

    @Test
    void assertStudentCanAccessLesson_throws_whenNotEnrolled() {
        when(enrollmentChecker.isStudentEnrolled(5L, 10L)).thenReturn(false);
        Lesson l = lesson(PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);

        assertThatThrownBy(() -> learningAccessService.assertStudentCanAccessLesson(l, student()))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void assertStudentCanAccessLesson_throws_whenCourseIsDraft() {
        when(enrollmentChecker.isStudentEnrolled(5L, 10L)).thenReturn(true);
        Lesson l = lesson(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);

        assertThatThrownBy(() -> learningAccessService.assertStudentCanAccessLesson(l, student()))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void assertStudentCanAccessLesson_throws_whenModuleIsDraft() {
        when(enrollmentChecker.isStudentEnrolled(5L, 10L)).thenReturn(true);
        Lesson l = lesson(PublicationStatus.PUBLISHED, PublicationStatus.DRAFT, PublicationStatus.PUBLISHED);

        assertThatThrownBy(() -> learningAccessService.assertStudentCanAccessLesson(l, student()))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void assertStudentCanAccessLesson_throws_whenLessonIsDraft() {
        when(enrollmentChecker.isStudentEnrolled(5L, 10L)).thenReturn(true);
        Lesson l = lesson(PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED, PublicationStatus.DRAFT);

        assertThatThrownBy(() -> learningAccessService.assertStudentCanAccessLesson(l, student()))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
