package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.UserSummary;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
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
