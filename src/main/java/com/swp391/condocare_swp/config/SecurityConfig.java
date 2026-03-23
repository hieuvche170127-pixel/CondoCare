package com.swp391.condocare_swp.config;

import com.swp391.condocare_swp.security.JwtAuthenticationFilter;
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
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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

                        // STAFF PAGES
                        .requestMatchers("/dashboard", "/dashboard/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STAFF")

                        // STAFF API — guard chung, @PreAuthorize xử lý chi tiết
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

                        // RESIDENT PAGES + API
                        .requestMatchers("/resident", "/resident/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")
                        .requestMatchers("/api/resident/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")

                        // PROFILE — tất cả đã đăng nhập
                        .requestMatchers("/profile", "/profile/**", "/api/profile/**")
                        .authenticated()

                        // MOMO
                        .requestMatchers("/api/auth/**", "/login", "/register").permitAll()
                        .requestMatchers("/api/momo/ipn").permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}