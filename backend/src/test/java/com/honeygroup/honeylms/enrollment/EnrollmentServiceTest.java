package com.honeygroup.honeylms.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseCategory;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.PublicationStatus;
import com.honeygroup.honeylms.enrollment.dto.EnrolledCourse;
import com.honeygroup.honeylms.enrollment.dto.EnrollmentResponse;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private UserAccount student() {
        return UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
    }

    private Course course(PublicationStatus status) {
        return Course.builder().id(10L).title("Anglais").description("desc")
                .category(CourseCategory.LANGUAGES).status(status).build();
    }

    @Test
    void enroll_savesEnrollment_whenCoursePublishedAndNotYetEnrolled() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(PublicationStatus.PUBLISHED)));
        when(userAccountRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student()));
        when(enrollmentRepository.existsByStudent_IdAndCourse_Id(5L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = enrollmentService.enroll(10L, "student@example.com");

        assertThat(result.courseId()).isEqualTo(10L);
        assertThat(result.enrolledAt()).isNotNull();
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void enroll_throwsAlreadyEnrolled_whenEnrollmentExists() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(PublicationStatus.PUBLISHED)));
        when(userAccountRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student()));
        when(enrollmentRepository.existsByStudent_IdAndCourse_Id(5L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(10L, "student@example.com"))
                .isInstanceOf(AlreadyEnrolledException.class);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_throwsForbidden_whenCourseIsDraft() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(PublicationStatus.DRAFT)));

        assertThatThrownBy(() -> enrollmentService.enroll(10L, "student@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_throwsCourseNotFound_whenCourseUnknown() {
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enroll(404L, "student@example.com"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void listMyCourses_returnsEnrolledCoursesOfTheStudent() {
        UserAccount student = student();
        Course course = course(PublicationStatus.PUBLISHED);
        when(userAccountRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(5L))
                .thenReturn(List.of(Enrollment.of(student, course)));

        List<EnrolledCourse> result = enrollmentService.listMyCourses("student@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Anglais");
        assertThat(result.get(0).category()).isEqualTo("LANGUAGES");
    }
}
