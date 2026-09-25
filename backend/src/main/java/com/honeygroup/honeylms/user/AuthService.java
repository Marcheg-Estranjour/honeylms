package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.RegisterRequest;
import com.honeygroup.honeylms.user.dto.RegisterResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository userAccountRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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
}
