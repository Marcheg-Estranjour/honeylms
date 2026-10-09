package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.CreateTrainerRequest;
import com.honeygroup.honeylms.user.dto.UpdateAccountStatusRequest;
import com.honeygroup.honeylms.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /** US-ADMIN-06 — Create a Trainer account. The role is imposed by the server. */
    @PostMapping("/trainers")
    public ResponseEntity<UserSummary> createTrainer(@Valid @RequestBody CreateTrainerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAccountService.createTrainer(request));
    }

    /**
     * US-AUTH-03 — Account status.
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserSummary> updateStatus(@PathVariable Long userId,
                                                      @Valid @RequestBody UpdateAccountStatusRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(userAccountService.updateStatus(userId, request.active(), authentication.getName()));
    }
}
