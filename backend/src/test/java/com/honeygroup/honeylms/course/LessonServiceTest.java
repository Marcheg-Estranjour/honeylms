package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.CreateLessonRequest;
import com.honeygroup.honeylms.course.dto.LessonDetail;
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
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @Mock
    private LearningAccessService learningAccessService;

    @InjectMocks
    private LessonService lessonService;

    private UserAccount trainer() {
        return UserAccount.builder().id(1L).email("trainer1@example.com")
                .role(new Role(2L, "TRAINER", "Trainer")).build();
    }

    private CourseModule module() {
        Course course = Course.builder().id(10L).title("Anglais").category(CourseCategory.LANGUAGES).build();
        return CourseModule.builder().id(100L).course(course).title("A1").displayOrder(1)
                .status(PublicationStatus.DRAFT).build();
    }

    @Test
    void createLesson_computesNextDisplayOrder_andSavesAsDraft() {
        UserAccount trainer = trainer();
        CourseModule module = module();
        CreateLessonRequest request = new CreateLessonRequest("Greetings", "desc", "Hello, hi...");

        when(courseModuleRepository.findById(100L)).thenReturn(Optional.of(module));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(lessonRepository.countByCourseModule_Id(100L)).thenReturn(0L);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(1000L);
            return l;
        });

        LessonDetail result = lessonService.createLesson(100L, request, "trainer1@example.com");

        assertThat(result.displayOrder()).isEqualTo(1);
        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.moduleId()).isEqualTo(100L);
    }

    @Test
    void createLesson_propagatesForbidden_whenPerimeterCheckFails() {
        UserAccount trainer = trainer();
        CourseModule module = module();
        CreateLessonRequest request = new CreateLessonRequest("Greetings", "desc", "content");

        when(courseModuleRepository.findById(100L)).thenReturn(Optional.of(module));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("nope"))
                .when(courseAuthorizationService).assertCanManageCourse(module.getCourse(), trainer);

        assertThatThrownBy(() -> lessonService.createLesson(100L, request, "trainer1@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void createLesson_throwsModuleNotFound_whenModuleUnknown() {
        when(courseModuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.createLesson(
                999L, new CreateLessonRequest("x", "y", "z"), "trainer1@example.com"))
                .isInstanceOf(ModuleNotFoundException.class);
    }

    @Test
    void publishLesson_setsStatusToPublished() {
        UserAccount trainer = trainer();
        Lesson lesson = Lesson.builder()
                .id(1000L).courseModule(module()).title("Greetings").displayOrder(1)
                .status(PublicationStatus.DRAFT).build();

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonDetail result = lessonService.publishLesson(1000L, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void getLesson_throwsLessonNotFound_whenIdUnknown() {
        when(lessonRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getLesson(9999L, "trainer1@example.com"))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void getLesson_forStudent_returnsLesson_whenAccessAllowed() {
        UserAccount student = UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        Lesson lesson = Lesson.builder().id(1000L).courseModule(module()).title("Greetings")
                .displayOrder(1).status(PublicationStatus.PUBLISHED).build();

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);

        LessonDetail result = lessonService.getLesson(1000L, "student@example.com");

        assertThat(result.title()).isEqualTo("Greetings");
        verify(learningAccessService).assertStudentCanAccessLesson(lesson, student);
    }

    @Test
    void getLesson_forStudent_propagatesForbidden_whenNotEnrolled() {
        UserAccount student = UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        Lesson lesson = Lesson.builder().id(1000L).courseModule(module()).title("Greetings")
                .displayOrder(1).status(PublicationStatus.PUBLISHED).build();

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);
        doThrow(new ForbiddenActionException("no access"))
                .when(learningAccessService).assertStudentCanAccessLesson(lesson, student);

        assertThatThrownBy(() -> lessonService.getLesson(1000L, "student@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void listLessons_forStudent_returnsOnlyPublishedLessons() {
        UserAccount student = UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        CourseModule module = module();
        Lesson published = Lesson.builder().id(1L).courseModule(module).title("L1")
                .displayOrder(1).status(PublicationStatus.PUBLISHED).build();
        Lesson draft = Lesson.builder().id(2L).courseModule(module).title("L2")
                .displayOrder(2).status(PublicationStatus.DRAFT).build();

        when(courseModuleRepository.findById(100L)).thenReturn(Optional.of(module));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(lessonRepository.findByCourseModule_IdOrderByDisplayOrderAsc(100L)).thenReturn(List.of(published, draft));
        when(learningAccessService.isStudent(student)).thenReturn(true);

        List<LessonDetail> result = lessonService.listLessons(100L, "student@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("L1");
    }
}
