package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.UpdateAccountStatusRequest;
import com.honeygroup.honeylms.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN only - see SecurityConfig. */
@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /** US-ADMIN-01 — Manage users. */
    @GetMapping
    public ResponseEntity<List<UserSummary>> listUsers() {
        return ResponseEntity.ok(userAccountService.listUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserSummary> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userAccountService.getUser(userId));
    }

    /**
     * US-AUTH-03 — Account status.
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserSummary> updateStatus(@PathVariable Long userId,
                                                      @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(userAccountService.updateStatus(userId, request.active()));
    }
}
