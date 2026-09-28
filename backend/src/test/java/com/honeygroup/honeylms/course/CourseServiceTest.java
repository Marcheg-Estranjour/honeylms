package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.CourseDetail;
import com.honeygroup.honeylms.course.dto.CourseSummary;
import com.honeygroup.honeylms.course.dto.CreateCourseRequest;
import com.honeygroup.honeylms.course.dto.UpdateCourseRequest;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Perimeter checks themselves (admin passes / assigned trainer passes / unassigned
 * trainer forbidden) are tested once, in CourseAuthorizationServiceTest - here we
 * only verify that CourseService delegates to it and behaves correctly around that.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseTrainerRepository courseTrainerRepository;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @InjectMocks
    private CourseService courseService;

    private UserAccount trainer(long id) {
        return UserAccount.builder().id(id).email("trainer" + id + "@example.com")
                .role(new Role(2L, "TRAINER", "Trainer")).build();
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

    @Test
    void getPublishedCourses_returnsAllPublished_whenNoCategoryGiven() {
        Course published = course(10L, PublicationStatus.PUBLISHED, trainer(1L));
        when(courseRepository.findByStatus(PublicationStatus.PUBLISHED)).thenReturn(List.of(published));

        List<CourseSummary> result = courseService.getPublishedCourses(Optional.empty());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Anglais");
    }

    @Test
    void createCourse_savesAsDraftAndRegistersTrainer_whenRequesterIsTrainer() {
        UserAccount trainer = trainer(1L);
        CreateCourseRequest request = new CreateCourseRequest("Anglais", "desc", CourseCategory.LANGUAGES);

        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CourseDetail result = courseService.createCourse(request, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("DRAFT");
        verify(courseTrainerRepository).save(any(CourseTrainer.class));
    }

    @Test
    void updateCourse_delegatesPerimeterCheck_andUpdatesFields() {
        UserAccount trainer = trainer(1L);
        Course existing = course(10L, PublicationStatus.DRAFT, trainer);
        UpdateCourseRequest request = new UpdateCourseRequest("Anglais avancé", "desc2", CourseCategory.LANGUAGES);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseDetail result = courseService.updateCourse(10L, request, "trainer1@example.com");

        assertThat(result.title()).isEqualTo("Anglais avancé");
        verify(courseAuthorizationService).assertCanManageCourse(existing, trainer);
    }

    @Test
    void updateCourse_propagatesForbidden_whenPerimeterCheckFails() {
        UserAccount trainer = trainer(2L);
        Course existing = course(10L, PublicationStatus.DRAFT, trainer(1L));
        UpdateCourseRequest request = new UpdateCourseRequest("Hack", "desc", CourseCategory.LANGUAGES);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseAuthorizationService.resolveRequester("trainer2@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("You are not allowed to manage this course"))
                .when(courseAuthorizationService).assertCanManageCourse(existing, trainer);

        assertThatThrownBy(() -> courseService.updateCourse(10L, request, "trainer2@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void publishCourse_setsStatusToPublished() {
        UserAccount trainer = trainer(1L);
        Course existing = course(10L, PublicationStatus.DRAFT, trainer);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseDetail result = courseService.publishCourse(10L, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void getCourseDetail_returnsDetail_whenPublishedAndRequesterAnonymous() {
        Course published = course(10L, PublicationStatus.PUBLISHED, trainer(1L));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(published));

        CourseDetail result = courseService.getCourseDetail(10L, null);

        assertThat(result.status()).isEqualTo("PUBLISHED");
        verifyNoInteractions(courseAuthorizationService);
    }

    @Test
    void getCourseDetail_throwsForbidden_whenDraftAndRequesterAnonymous() {
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
