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
 * which is enough for simple, role-only cases ("ADMIN only", "TRAINER or ADMIN can
 * POST"). Data-dependent ownership/perimeter checks (e.g. "this Trainer owns this
 * Course") cannot be expressed as a URL matcher - they live in CourseAuthorizationService
 * and are called explicitly from CourseService/CourseModuleService/LessonService.
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
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/*").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/courses/*/publish").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/modules").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/modules/*").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/modules/*/publish").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/modules/*/lessons").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/lessons/*").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/lessons/*/publish").hasAnyRole("TRAINER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/enrollment").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/me/courses").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/status").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
