package com.honeygroup.honeylms.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.security.JwtService;
import com.honeygroup.honeylms.user.dto.AuthResponse;
import com.honeygroup.honeylms.user.dto.LoginRequest;
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

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Role studentRole;

    @BeforeEach
    void setUp() {
        studentRole = new Role(1L, "STUDENT", "Student");
    }

    // ---- register() ----

    @Test
    void register_createsAccountWithStudentRole_whenEmailIsAvailable() {
        RegisterRequest request = new RegisterRequest("Alice@Example.com", "password123", "Alice", "Martin");

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
        assertThat(response.role()).isEqualTo("STUDENT");
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailIsTaken() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123", "Alice", "Martin");
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void register_throwsIllegalState_whenStudentRoleIsMissing() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123", "Alice", "Martin");
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class);

        verify(userAccountRepository, never()).save(any());
    }

    // ---- login() ----

    private UserAccount activeAccount() {
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
    void login_returnsToken_whenCredentialsAreValidAndAccountActive() {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        UserAccount account = activeAccount();

        when(userAccountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(account)).thenReturn("fake.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("fake.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.user().role()).isEqualTo("STUDENT");
    }

    @Test
    void login_throwsInvalidCredentials_whenEmailUnknown() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrong-password");
        UserAccount account = activeAccount();

        when(userAccountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsAccountDisabled_whenAccountIsInactive() {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        UserAccount account = activeAccount();
        account.setActive(false);

        when(userAccountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountDisabledException.class);
    }
}
