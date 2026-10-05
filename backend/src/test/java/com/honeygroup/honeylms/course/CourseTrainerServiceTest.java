package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import com.honeygroup.honeylms.user.UserNotFoundException;
import com.honeygroup.honeylms.user.dto.UserSummary;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseTrainerServiceTest {

    @Mock
    private CourseTrainerRepository courseTrainerRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @InjectMocks
    private CourseTrainerService courseTrainerService;

    private UserAccount admin() {
        return UserAccount.builder().id(99L).email("admin@example.com")
                .role(new Role(3L, "ADMIN", "Admin")).build();
    }

    private UserAccount user(long id, String roleCode) {
        return UserAccount.builder().id(id).email("u" + id + "@example.com").firstName("F").lastName("L")
                .role(new Role(1L, roleCode, roleCode)).build();
    }

    private Course course() {
        return Course.builder().id(10L).title("Anglais").build();
    }

    private void adminRequester() {
        when(courseAuthorizationService.resolveRequester("admin@example.com")).thenReturn(admin());
    }

    @Test
    void assignTrainer_savesAssignment_whenValid() {
        adminRequester();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course()));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(user(2L, "TRAINER")));
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 2L)).thenReturn(false);

        courseTrainerService.assignTrainer(10L, 2L, "admin@example.com");

        verify(courseTrainerRepository).save(any(CourseTrainer.class));
    }

    @Test
    void assignTrainer_throwsInvalidAssignment_whenUserIsNotATrainer() {
        adminRequester();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course()));
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(user(5L, "STUDENT")));

        assertThatThrownBy(() -> courseTrainerService.assignTrainer(10L, 5L, "admin@example.com"))
                .isInstanceOf(InvalidTrainerAssignmentException.class);
        verify(courseTrainerRepository, never()).save(any());
    }

    @Test
    void assignTrainer_throwsAlreadyAssigned_whenAssignmentExists() {
        adminRequester();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course()));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(user(2L, "TRAINER")));
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> courseTrainerService.assignTrainer(10L, 2L, "admin@example.com"))
                .isInstanceOf(TrainerAlreadyAssignedException.class);
    }

    @Test
    void assignTrainer_throwsUserNotFound_whenTrainerUnknown() {
        adminRequester();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course()));
        when(userAccountRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseTrainerService.assignTrainer(10L, 404L, "admin@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void assignTrainer_throwsCourseNotFound_whenCourseUnknown() {
        adminRequester();
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseTrainerService.assignTrainer(404L, 2L, "admin@example.com"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void assignTrainer_propagatesForbidden_whenRequesterIsNotAdmin() {
        UserAccount trainer = user(2L, "TRAINER");
        when(courseAuthorizationService.resolveRequester("u2@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("Only an Admin can perform this action"))
                .when(courseAuthorizationService).assertAdmin(trainer);

        assertThatThrownBy(() -> courseTrainerService.assignTrainer(10L, 2L, "u2@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
        verify(courseTrainerRepository, never()).save(any());
    }

    @Test
    void unassignTrainer_deletesAssignment_whenItExists() {
        adminRequester();
        Course course = course();
        UserAccount trainer = user(2L, "TRAINER");
        CourseTrainer assignment = CourseTrainer.of(course, trainer);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseTrainerRepository.findById(new CourseTrainerId(10L, 2L))).thenReturn(Optional.of(assignment));

        courseTrainerService.unassignTrainer(10L, 2L, "admin@example.com");

        verify(courseTrainerRepository).delete(assignment);
    }

    @Test
    void unassignTrainer_throwsNotFound_whenNotAssigned() {
        adminRequester();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course()));
        when(courseTrainerRepository.findById(new CourseTrainerId(10L, 2L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseTrainerService.unassignTrainer(10L, 2L, "admin@example.com"))
                .isInstanceOf(TrainerAssignmentNotFoundException.class);
    }

    @Test
    void listTrainers_returnsAssignedTrainersAsSummaries() {
        adminRequester();
        Course course = course();
        UserAccount trainer = user(2L, "TRAINER");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseTrainerRepository.findByCourse_Id(10L)).thenReturn(List.of(CourseTrainer.of(course, trainer)));

        List<UserSummary> result = courseTrainerService.listTrainers(10L, "admin@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(0).role()).isEqualTo("TRAINER");
    }
}
