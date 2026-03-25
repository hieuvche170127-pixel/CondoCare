package com.swp391.condocare_swp.config;

import com.swp391.condocare_swp.security.CustomUserDetailsService;
import com.swp391.condocare_swp.security.JwtAuthenticationFilter;
import com.swp391.condocare_swp.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            CustomUserDetailsService customUserDetailsService) {
        return new JwtAuthenticationFilter(tokenProvider, customUserDetailsService);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth

                        // ── PUBLIC ──────────────────────────────────────────────────
                        .requestMatchers(
                                "/", "/login", "/register", "/forgot-password", "/reset-password",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/api/auth/**"
                        ).permitAll()

                        // ── TRANG HTML (JS tự check login) ──────────────────────────
                        .requestMatchers(
                                "/dashboard", "/dashboard/**",
                                "/resident",  "/resident/**",
                                "/profile",   "/profile/**"
                        ).permitAll()

                        // ── STAFF API ────────────────────────────────────────────────
                        .requestMatchers("/api/staff-management/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/resident-management/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/invoice-management/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/staff/requests/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/staff/vehicles/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/dashboard/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")

                        // ── RESIDENT API ─────────────────────────────────────────────
                        .requestMatchers("/api/resident/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")

                        // ── PROFILE API ──────────────────────────────────────────────
                        .requestMatchers("/api/profile/**")
                        .authenticated()

                        // ── MOMO (IPN không cần auth) ────────────────────────────────
                        .requestMatchers("/api/momo/ipn", "/api/momo/return").permitAll()
                        .requestMatchers("/api/momo/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}