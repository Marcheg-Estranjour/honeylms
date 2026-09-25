package com.honeygroup.honeylms.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.user.dto.RegisterRequest;
import com.honeygroup.honeylms.user.dto.RegisterResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest request;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest("Alice@Example.com", "password123", "Alice", "Martin");
        studentRole = new Role(1L, "STUDENT", "Student");
    }

    @Test
    void register_createsAccountWithStudentRole_whenEmailIsAvailable() {
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setId(42L);
            return account;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("alice@example.com"); // normalisé en minuscules
        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.role()).isEqualTo("STUDENT");
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailIsTaken() {
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void register_throwsIllegalState_whenStudentRoleIsMissing() {
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class);

        verify(userAccountRepository, never()).save(any());
    }
}
