package com.honeygroup.honeylms.user;

import com.honeygroup.honeylms.user.dto.AuthResponse;
import com.honeygroup.honeylms.user.dto.LoginRequest;
import com.honeygroup.honeylms.user.dto.RegisterRequest;
import com.honeygroup.honeylms.user.dto.RegisterResponse;
import com.honeygroup.honeylms.user.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * US-AUTH-01 — Registration. Publicly accessible (see SecurityConfig).
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * US-AUTH-02 — Login. Publicly accessible. Returns a JWT valid 24h (see JwtProperties).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Returns the currently authenticated user. Requires a valid "Authorization: Bearer <token>"
     * header - JwtAuthenticationFilter populates `authentication` from it.
     */
    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }
}
