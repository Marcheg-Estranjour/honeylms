package com.honeygroup.honeylms.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.user.dto.CreateTrainerRequest;
import com.honeygroup.honeylms.user.dto.UserSummary;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountService userAccountService;

    private UserAccount account() {
        Role studentRole = new Role(1L, "STUDENT", "Student");
        return UserAccount.builder()
                .id(1L)
                .email("alice@example.com")
                .passwordHash("hashed-password")
                .firstName("Alice")
                .lastName("Martin")
                .role(studentRole)
                .active(true)
                .build();
    }

    @Test
    void createTrainer_forcesTrainerRole_hashesPassword_andNormalizesEmail() {
        Role trainerRole = new Role(2L, "TRAINER", "Trainer");
        CreateTrainerRequest request = new CreateTrainerRequest("Paul@Example.com", "password123", " Paul ", "Durand");

        when(userAccountRepository.existsByEmail("paul@example.com")).thenReturn(false);
        when(roleRepository.findByCode("TRAINER")).thenReturn(Optional.of(trainerRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount a = inv.getArgument(0);
            a.setId(7L);
            return a;
        });

        UserSummary result = userAccountService.createTrainer(request);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.email()).isEqualTo("paul@example.com");
        assertThat(result.firstName()).isEqualTo("Paul");
        assertThat(result.role()).isEqualTo("TRAINER");
    }

    @Test
    void createTrainer_throwsEmailAlreadyExists_andSavesNothing_whenEmailTaken() {
        CreateTrainerRequest request = new CreateTrainerRequest("paul@example.com", "password123", "Paul", "Durand");
        when(userAccountRepository.existsByEmail("paul@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userAccountService.createTrainer(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void listUsers_returnsAllAccountsAsSummaries() {
        when(userAccountRepository.findAll()).thenReturn(List.of(account()));

        List<UserSummary> result = userAccountService.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("alice@example.com");
        assertThat(result.get(0).role()).isEqualTo("STUDENT");
    }

    @Test
    void getUser_returnsSummary_whenIdExists() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account()));

        UserSummary result = userAccountService.getUser(1L);

        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    void getUser_throwsUserNotFound_whenIdUnknown() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountService.getUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateStatus_disablesAccount_whenActiveIsFalse() {
        UserAccount account = account();
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummary result = userAccountService.updateStatus(1L, false);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(account.isActive()).isFalse();
    }

    @Test
    void updateStatus_throwsUserNotFound_whenIdUnknown() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountService.updateStatus(99L, false))
                .isInstanceOf(UserNotFoundException.class);
    }
}
