package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAuthorizationServiceTest {

    @Mock
    private CourseTrainerRepository courseTrainerRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CourseAuthorizationService courseAuthorizationService;

    private UserAccount userWithRole(long id, String roleCode) {
        return UserAccount.builder().id(id).role(new Role(1L, roleCode, roleCode)).build();
    }

    @Test
    void assertCanManageCourse_alwaysPasses_forAdmin() {
        Course course = Course.builder().id(10L).build();
        UserAccount admin = userWithRole(1L, "ADMIN");

        courseAuthorizationService.assertCanManageCourse(course, admin); // no exception
    }

    @Test
    void assertCanManageCourse_passes_whenTrainerIsAssigned() {
        Course course = Course.builder().id(10L).build();
        UserAccount trainer = userWithRole(2L, "TRAINER");
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 2L)).thenReturn(true);

        courseAuthorizationService.assertCanManageCourse(course, trainer); // no exception
    }

    @Test
    void assertCanManageCourse_throws_whenTrainerIsNotAssigned() {
        Course course = Course.builder().id(10L).build();
        UserAccount trainer = userWithRole(3L, "TRAINER");
        when(courseTrainerRepository.existsByCourse_IdAndTrainer_Id(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> courseAuthorizationService.assertCanManageCourse(course, trainer))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void assertAdmin_passes_forAdmin() {
        courseAuthorizationService.assertAdmin(userWithRole(1L, "ADMIN")); // no exception
    }

    @Test
    void assertAdmin_throws_forTrainerAndStudent() {
        assertThatThrownBy(() -> courseAuthorizationService.assertAdmin(userWithRole(2L, "TRAINER")))
                .isInstanceOf(ForbiddenActionException.class);
        assertThatThrownBy(() -> courseAuthorizationService.assertAdmin(userWithRole(3L, "STUDENT")))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void resolveRequester_returnsAccount_whenEmailExists() {
        UserAccount account = userWithRole(1L, "TRAINER");
        when(userAccountRepository.findByEmail("trainer@example.com")).thenReturn(Optional.of(account));

        UserAccount result = courseAuthorizationService.resolveRequester("trainer@example.com");

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void resolveRequester_throws_whenEmailUnknown() {
        when(userAccountRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseAuthorizationService.resolveRequester("ghost@example.com"))
                .isInstanceOf(IllegalStateException.class);
    }
}
