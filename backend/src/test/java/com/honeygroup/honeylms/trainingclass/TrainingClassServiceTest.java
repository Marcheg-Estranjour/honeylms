package com.honeygroup.honeylms.trainingclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.CourseTrainerRepository;
import com.honeygroup.honeylms.trainingclass.dto.ClassMembersResponse;
import com.honeygroup.honeylms.trainingclass.dto.CreateClassRequest;
import com.honeygroup.honeylms.trainingclass.dto.TrainingClassDetail;
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
class TrainingClassServiceTest {

    @Mock
    private TrainingClassRepository trainingClassRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private ClassTrainerRepository classTrainerRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseTrainerRepository courseTrainerRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @InjectMocks
    private TrainingClassService trainingClassService;

    private UserAccount trainer(long id) {
        return UserAccount.builder().id(id).email("trainer" + id + "@example.com")
                .role(new Role(2L, "TRAINER", "Trainer")).build();
    }

    private UserAccount student(long id) {
        return UserAccount.builder().id(id).email("student" + id + "@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
    }

    private TrainingClass trainingClass() {
        Course course = Course.builder().id(10L).build();
        return TrainingClass.builder().id(50L).course(course).name("Anglais Septembre").build();
    }

    @Test
    void createClass_savesClass_whenAuthorized() {
        UserAccount trainer = trainer(1L);
        Course course = Course.builder().id(10L).build();
        CreateClassRequest request = new CreateClassRequest("Anglais Septembre", "Promo rentrée");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(trainingClassRepository.save(any(TrainingClass.class))).thenAnswer(inv -> {
            TrainingClass tc = inv.getArgument(0);
            tc.setId(50L);
            return tc;
        });

        TrainingClassDetail result = trainingClassService.createClass(10L, request, "trainer1@example.com");

        assertThat(result.name()).isEqualTo("Anglais Septembre");
        assertThat(result.courseId()).isEqualTo(10L);
    }

    @Test
    void createClass_throwsCourseNotFound_whenCourseUnknown() {
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingClassService.createClass(
                404L, new CreateClassRequest("x", "y"), "trainer1@example.com"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void addStudent_addsMembership_whenValid() {
        TrainingClass tc = trainingClass();
        UserAccount trainer = trainer(1L);
        UserAccount student = student(5L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(student));
        when(classStudentRepository.existsByTrainingClass_IdAndStudent_Id(50L, 5L)).thenReturn(false);

        trainingClassService.addStudent(50L, 5L, "trainer1@example.com");

        verify(classStudentRepository).save(any(ClassStudent.class));
    }

    @Test
    void addStudent_throwsInvalidAssignment_whenUserIsNotAStudent() {
        TrainingClass tc = trainingClass();
        UserAccount trainer = trainer(1L);
        UserAccount notAStudent = trainer(2L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(notAStudent));

        assertThatThrownBy(() -> trainingClassService.addStudent(50L, 2L, "trainer1@example.com"))
                .isInstanceOf(InvalidClassAssignmentException.class);
    }

    @Test
    void addStudent_throwsAlreadyMember_whenAlreadyInClass() {
        TrainingClass tc = trainingClass();
        UserAccount trainer = trainer(1L);
        UserAccount student = student(5L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(student));
        when(classStudentRepository.existsByTrainingClass_IdAndStudent_Id(50L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> trainingClassService.addStudent(50L, 5L, "trainer1@example.com"))
                .isInstanceOf(AlreadyClassMemberException.class);
    }

    @Test
    void addStudent_propagatesForbidden_whenTrainerOutsidePerimeter() {
        TrainingClass tc = trainingClass();
        UserAccount trainer = trainer(2L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer2@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("nope"))
                .when(courseAuthorizationService).assertCanManageCourse(tc.getCourse(), trainer);

        assertThatThrownBy(() -> trainingClassService.addStudent(50L, 5L, "trainer2@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void addTrainer_addsMembership_whenTrainerAlreadyInCoursePerimeter() {
        TrainingClass tc = trainingClass();
        UserAccount requester = trainer(1L);
        UserAccount newTrainer = trainer(2L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(requester);
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(newTrainer));
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 2L)).thenReturn(true);
        when(classTrainerRepository.existsByTrainingClass_IdAndTrainer_Id(50L, 2L)).thenReturn(false);

        trainingClassService.addTrainer(50L, 2L, "trainer1@example.com");

        verify(classTrainerRepository).save(any(ClassTrainer.class));
    }

    @Test
    void addTrainer_throwsInvalidAssignment_whenTrainerNotInCoursePerimeter() {
        TrainingClass tc = trainingClass();
        UserAccount requester = trainer(1L);
        UserAccount outsideTrainer = trainer(3L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(requester);
        when(userAccountRepository.findById(3L)).thenReturn(Optional.of(outsideTrainer));
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> trainingClassService.addTrainer(50L, 3L, "trainer1@example.com"))
                .isInstanceOf(InvalidClassAssignmentException.class);
    }

    @Test
    void removeStudent_throwsMembershipNotFound_whenNotAMember() {
        TrainingClass tc = trainingClass();
        UserAccount trainer = trainer(1L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(classStudentRepository.findByTrainingClass_IdAndStudent_Id(50L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingClassService.removeStudent(50L, 5L, "trainer1@example.com"))
                .isInstanceOf(ClassMembershipNotFoundException.class);
    }

    @Test
    void getMembers_returnsStudentsAndTrainers() {
        TrainingClass tc = trainingClass();
        UserAccount trainer = trainer(1L);
        UserAccount student = student(5L);
        UserAccount classTrainerAccount = trainer(2L);

        when(trainingClassRepository.findById(50L)).thenReturn(Optional.of(tc));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(classStudentRepository.findByTrainingClass_Id(50L))
                .thenReturn(List.of(ClassStudent.of(tc, student)));
        when(classTrainerRepository.findByTrainingClass_Id(50L))
                .thenReturn(List.of(ClassTrainer.of(tc, classTrainerAccount)));

        ClassMembersResponse result = trainingClassService.getMembers(50L, "trainer1@example.com");

        assertThat(result.students()).hasSize(1);
        assertThat(result.trainers()).hasSize(1);
    }

    @Test
    void findClass_throwsTrainingClassNotFound_whenIdUnknown() {
        when(trainingClassRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingClassService.getMembers(404L, "trainer1@example.com"))
                .isInstanceOf(TrainingClassNotFoundException.class);
    }
}
