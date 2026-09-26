package com.honeygroup.honeylms.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.user.dto.UserSummary;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

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
