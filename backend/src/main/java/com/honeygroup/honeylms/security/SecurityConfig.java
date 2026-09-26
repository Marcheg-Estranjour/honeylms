package com.honeygroup.honeylms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 7 configuration (mandatory Lambda DSL - no WebSecurityConfigurerAdapter,
 * no .and() chaining). API REST stateless : no session, no CSRF, no form login.
 *
 * Only /api/auth/register and /api/auth/login are public. Everything else -
 * including /api/auth/me - requires a valid JWT, checked by JwtAuthenticationFilter
 * before Spring Security's own UsernamePasswordAuthenticationFilter runs.
 *
 * Role-based rules are declared here at the URL level (requestMatchers().hasRole(...)),
 * which is enough for simple cases like "ADMIN only". Ownership/perimeter checks that
 * depend on data (e.g. "this Trainer owns this Course") will need @PreAuthorize +
 * @EnableMethodSecurity, or a dedicated check inside the service - added when that
 * story comes up rather than now.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/status").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
