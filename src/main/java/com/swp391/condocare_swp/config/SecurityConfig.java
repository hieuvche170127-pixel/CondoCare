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

/**
 * SecurityConfig — phân quyền theo 2 tầng:
 *
 *  Tầng 1 (SecurityConfig): kiểm tra URL → phải là STAFF hoặc RESIDENT
 *  Tầng 2 (@PreAuthorize): kiểm tra method → ADMIN/MANAGER/STAFF cụ thể
 *
 * Roles:
 *  Staff    → ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF   (từ Role.name)
 *  Resident → ROLE_OWNER, ROLE_TENANT, ROLE_GUEST    (từ Residents.type)
 */
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
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {   // ← THÊM DÒNG NÀY (quan trọng)

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth

                        // PUBLIC
                        .requestMatchers(
                                "/", "/login", "/register", "/forgot-password", "/reset-password",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico", "/api/auth/**"
                        ).permitAll()

                        // === TRANG HTML ĐƯỢC PHÉP TRUY CẬP (JS tự check login) ===
                        .requestMatchers("/dashboard", "/dashboard/**",
                                "/resident", "/resident/**",
                                "/profile", "/profile/**")
                        .permitAll()

                        // STAFF API
                        .requestMatchers("/api/staff-management/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/resident-management/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/invoice-management/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/staff/requests/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/dashboard/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")

                        // RESIDENT API
                        .requestMatchers("/api/resident/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")

                        // PROFILE API
                        .requestMatchers("/api/profile/**")
                        .authenticated()

                        // MOMO
                        .requestMatchers("/api/momo/ipn", "/api/momo/return").permitAll()
                        .requestMatchers("/api/momo/create-payment", "/api/momo/status/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);  // ← Giờ không lỗi nữa

        return http.build();
    }
}