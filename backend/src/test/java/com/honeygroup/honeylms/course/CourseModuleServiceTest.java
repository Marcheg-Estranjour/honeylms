package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.CreateModuleRequest;
import com.honeygroup.honeylms.course.dto.ModuleDetail;
import com.honeygroup.honeylms.course.dto.UpdateModuleRequest;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseModuleServiceTest {

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @Mock
    private LearningAccessService learningAccessService;

    @InjectMocks
    private CourseModuleService courseModuleService;

    private UserAccount trainer() {
        return UserAccount.builder().id(1L).email("trainer1@example.com")
                .role(new Role(2L, "TRAINER", "Trainer")).build();
    }

    private Course course() {
        return Course.builder().id(10L).title("Anglais").category(CourseCategory.LANGUAGES).build();
    }

    @Test
    void createModule_computesNextDisplayOrder_andSavesAsDraft() {
        UserAccount trainer = trainer();
        Course course = course();
        CreateModuleRequest request = new CreateModuleRequest("A1", "Niveau débutant");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(courseModuleRepository.countByCourse_Id(10L)).thenReturn(2L);
        when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(inv -> {
            CourseModule m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        ModuleDetail result = courseModuleService.createModule(10L, request, "trainer1@example.com");

        assertThat(result.displayOrder()).isEqualTo(3); // 2 existants + 1
        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.courseId()).isEqualTo(10L);
    }

    @Test
    void createModule_propagatesForbidden_whenPerimeterCheckFails() {
        UserAccount trainer = trainer();
        Course course = course();
        CreateModuleRequest request = new CreateModuleRequest("A1", "desc");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("nope"))
                .when(courseAuthorizationService).assertCanManageCourse(course, trainer);

        assertThatThrownBy(() -> courseModuleService.createModule(10L, request, "trainer1@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void createModule_throwsCourseNotFound_whenCourseUnknown() {
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseModuleService.createModule(
                404L, new CreateModuleRequest("A1", "desc"), "trainer1@example.com"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void publishModule_setsStatusToPublished() {
        UserAccount trainer = trainer();
        CourseModule module = CourseModule.builder()
                .id(100L).course(course()).title("A1").displayOrder(1).status(PublicationStatus.DRAFT).build();

        when(courseModuleRepository.findById(100L)).thenReturn(Optional.of(module));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(inv -> inv.getArgument(0));

        ModuleDetail result = courseModuleService.publishModule(100L, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void updateModule_throwsModuleNotFound_whenIdUnknown() {
        when(courseModuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseModuleService.updateModule(
                999L, new UpdateModuleRequest("x", "y"), "trainer1@example.com"))
                .isInstanceOf(ModuleNotFoundException.class);
    }

    @Test
    void listModules_forStudent_returnsOnlyPublishedModules() {
        UserAccount student = UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        Course course = course();
        CourseModule published = CourseModule.builder().id(1L).course(course).title("A1")
                .displayOrder(1).status(PublicationStatus.PUBLISHED).build();
        CourseModule draft = CourseModule.builder().id(2L).course(course).title("A2")
                .displayOrder(2).status(PublicationStatus.DRAFT).build();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(courseModuleRepository.findByCourse_IdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(published, draft));
        when(learningAccessService.isStudent(student)).thenReturn(true);

        List<ModuleDetail> result = courseModuleService.listModules(10L, "student@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("A1");
        verify(learningAccessService).assertStudentCanAccessCourse(course, student);
    }

    @Test
    void getModule_forStudent_propagatesForbidden_whenNotAllowed() {
        UserAccount student = UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        CourseModule module = CourseModule.builder().id(100L).course(course()).title("A1")
                .displayOrder(1).status(PublicationStatus.PUBLISHED).build();

        when(courseModuleRepository.findById(100L)).thenReturn(Optional.of(module));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);
        doThrow(new ForbiddenActionException("no access"))
                .when(learningAccessService).assertStudentCanAccessModule(module, student);

        assertThatThrownBy(() -> courseModuleService.getModule(100L, "student@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
