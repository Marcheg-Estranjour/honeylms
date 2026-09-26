package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
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
        UserAccount saved = userAccountRepository.save(account);

        return new UserSummary(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole().getCode()
        );
    }
}
