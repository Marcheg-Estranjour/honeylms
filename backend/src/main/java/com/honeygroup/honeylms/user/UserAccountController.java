package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.UpdateAccountStatusRequest;
import com.honeygroup.honeylms.user.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * US-AUTH-03 — Account status. ADMIN only (enforced in SecurityConfig).
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserSummary> updateStatus(@PathVariable Long userId,
                                                      @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(userAccountService.updateStatus(userId, request.active()));
    }
}
