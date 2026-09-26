package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.security.JwtService;
import com.honeygroup.honeylms.user.dto.AuthResponse;
import com.honeygroup.honeylms.user.dto.LoginRequest;
import com.honeygroup.honeylms.user.dto.RegisterRequest;
import com.honeygroup.honeylms.user.dto.RegisterResponse;
import com.honeygroup.honeylms.user.dto.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository userAccountRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * US-AUTH-01 — Registration.
     * Creates a new account with role STUDENT, active by default.
     * The email is normalized (trimmed + lower-cased) so uniqueness is not
     * bypassable by case ("Alice@x.com" vs "alice@x.com").
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Role studentRole = roleRepository.findByCode(RoleCode.STUDENT.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Role STUDENT not found - check that V1__create_role.sql ran correctly"));

        UserAccount account = UserAccount.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(studentRole)
                .active(true)
                .build();

        UserAccount saved = userAccountRepository.save(account);

        return new RegisterResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                studentRole.getCode()
        );
    }

    /**
     * US-AUTH-02 — Login.
     * Order of checks matters for security: we never reveal whether the email
     * exists or the password was wrong (same InvalidCredentialsException for both) -
     * the account-disabled check only happens once the password is confirmed correct.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!account.isActive()) {
            throw new AccountDisabledException();
        }

        String token = jwtService.generateToken(account);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                toSummary(account)
        );
    }

    /**
     * Backs GET /api/auth/me - "email" comes from the JWT subject via JwtAuthenticationFilter.
     */
    @Transactional(readOnly = true)
    public UserSummary getCurrentUser(String email) {
        UserAccount account = userAccountRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        return toSummary(account);
    }

    private UserSummary toSummary(UserAccount account) {
        return new UserSummary(
                account.getId(),
                account.getEmail(),
                account.getFirstName(),
                account.getLastName(),
                account.getRole().getCode()
        );
    }
}
