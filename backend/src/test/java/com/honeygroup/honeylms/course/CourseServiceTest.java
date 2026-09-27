package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.CourseDetail;
import com.honeygroup.honeylms.course.dto.CourseSummary;
import com.honeygroup.honeylms.course.dto.CreateCourseRequest;
import com.honeygroup.honeylms.course.dto.UpdateCourseRequest;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseTrainerRepository courseTrainerRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CourseService courseService;

    private UserAccount trainer(long id) {
        return UserAccount.builder()
                .id(id)
                .email("trainer" + id + "@example.com")
                .role(new Role(2L, "TRAINER", "Trainer"))
                .build();
    }

    private UserAccount admin() {
        return UserAccount.builder()
                .id(99L)
                .email("admin@example.com")
                .role(new Role(3L, "ADMIN", "Admin"))
                .build();
    }

    private Course course(Long id, PublicationStatus status, UserAccount owner) {
        return Course.builder()
                .id(id)
                .title("Anglais")
                .description("desc")
                .status(status)
                .category(CourseCategory.LANGUAGES)
                .createdBy(owner)
                .build();
    }

    // ---- getPublishedCourses (existing behaviour, kept from Sprint 2 first commit) ----

    @Test
    void getPublishedCourses_returnsAllPublished_whenNoCategoryGiven() {
        Course published = course(10L, PublicationStatus.PUBLISHED, trainer(1L));
        when(courseRepository.findByStatus(PublicationStatus.PUBLISHED)).thenReturn(List.of(published));

        List<CourseSummary> result = courseService.getPublishedCourses(Optional.empty());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Anglais");
        verify(courseRepository).findByStatus(PublicationStatus.PUBLISHED);
    }

    // ---- createCourse ----

    @Test
    void createCourse_savesAsDraftAndRegistersTrainer_whenRequesterIsTrainer() {
        UserAccount trainer = trainer(1L);
        CreateCourseRequest request = new CreateCourseRequest("Anglais", "desc", CourseCategory.LANGUAGES);

        when(userAccountRepository.findByEmail("trainer1@example.com")).thenReturn(Optional.of(trainer));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CourseDetail result = courseService.createCourse(request, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.category()).isEqualTo("LANGUAGES");
        verify(courseTrainerRepository).save(any(CourseTrainer.class));
    }

    @Test
    void createCourse_doesNotRegisterCourseTrainer_whenRequesterIsAdmin() {
        UserAccount admin = admin();
        CreateCourseRequest request = new CreateCourseRequest("Anglais", "desc", CourseCategory.LANGUAGES);

        when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(11L);
            return c;
        });

        courseService.createCourse(request, "admin@example.com");

        verifyNoMoreInteractions(courseTrainerRepository);
    }

    // ---- updateCourse / publishCourse: perimeter checks ----

    @Test
    void updateCourse_succeeds_whenRequesterIsAssignedTrainer() {
        UserAccount trainer = trainer(1L);
        Course existing = course(10L, PublicationStatus.DRAFT, trainer);
        UpdateCourseRequest request = new UpdateCourseRequest("Anglais avancé", "desc2", CourseCategory.LANGUAGES);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userAccountRepository.findByEmail("trainer1@example.com")).thenReturn(Optional.of(trainer));
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 1L)).thenReturn(true);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseDetail result = courseService.updateCourse(10L, request, "trainer1@example.com");

        assertThat(result.title()).isEqualTo("Anglais avancé");
    }

    @Test
    void updateCourse_throwsForbidden_whenRequesterIsUnrelatedTrainer() {
        UserAccount owner = trainer(1L);
        UserAccount otherTrainer = trainer(2L);
        Course existing = course(10L, PublicationStatus.DRAFT, owner);
        UpdateCourseRequest request = new UpdateCourseRequest("Anglais avancé", "desc2", CourseCategory.LANGUAGES);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userAccountRepository.findByEmail("trainer2@example.com")).thenReturn(Optional.of(otherTrainer));
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> courseService.updateCourse(10L, request, "trainer2@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void publishCourse_succeeds_whenRequesterIsAdmin() {
        Course existing = course(10L, PublicationStatus.DRAFT, trainer(1L));

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin()));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseDetail result = courseService.publishCourse(10L, "admin@example.com");

        assertThat(result.status()).isEqualTo("PUBLISHED");
    }

    // ---- getCourseDetail ----

    @Test
    void getCourseDetail_returnsDetail_whenCoursePublishedAndRequesterAnonymous() {
        Course published = course(10L, PublicationStatus.PUBLISHED, trainer(1L));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(published));

        CourseDetail result = courseService.getCourseDetail(10L, null);

        assertThat(result.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void getCourseDetail_throwsForbidden_whenCourseDraftAndRequesterAnonymous() {
        Course draft = course(10L, PublicationStatus.DRAFT, trainer(1L));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> courseService.getCourseDetail(10L, null))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getCourseDetail_throwsCourseNotFound_whenIdUnknown() {
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseDetail(404L, null))
                .isInstanceOf(CourseNotFoundException.class);
    }
}
