package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.CreateTrainerRequest;
import com.honeygroup.honeylms.user.dto.UserSummary;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * US-ADMIN-06 — Create a Trainer account. VALIDÉ : the Admin creates Trainer
     * accounts from the admin interface. ADMIN only (SecurityConfig). The role is
     * forced to TRAINER here. HYPOTHÈSE RETENUE (option la plus simple) : the Admin
     * sets the initial password - no email/invitation flow in the MVP.
     */
    @Transactional
    public UserSummary createTrainer(CreateTrainerRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Role trainerRole = roleRepository.findByCode(RoleCode.TRAINER.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Role TRAINER not found - check that V1__create_role.sql ran correctly"));

        UserAccount account = UserAccount.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(trainerRole)
                .active(true)
                .build();

        return toSummary(userAccountRepository.save(account));
    }

    /** US-ADMIN-01 — Manage users. ADMIN only (see SecurityConfig). */
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userAccountRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummary getUser(Long userId) {
        return toSummary(userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)));
    }

    /**
     * US-AUTH-03 — Account status.
     * Authorization (ADMIN only) is enforced at the security layer (SecurityConfig),
     * not here - this service only implements the business action itself.
     */
    @Transactional
    public UserSummary updateStatus(Long userId, boolean active) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        account.setActive(active);
        return toSummary(userAccountRepository.save(account));
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
