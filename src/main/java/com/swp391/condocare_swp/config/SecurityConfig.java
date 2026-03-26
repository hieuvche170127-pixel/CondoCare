package com.swp391.condocare_swp.config;

import com.swp391.condocare_swp.security.CustomUserDetailsService;
import com.swp391.condocare_swp.security.JwtAuthenticationFilter;
import com.swp391.condocare_swp.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth

                        // ── 1. CORS preflight + PUBLIC (rất quan trọng) ─────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── 2. TẤT CẢ TRANG HTML AUTH + API AUTH ───────────────────────
                        .requestMatchers(
                                "/",
                                "/login", "/auth/login",
                                "/register", "/auth/register",
                                "/forgot-password", "/auth/forgot-password",
                                "/reset-password", "/auth/reset-password",
                                "/auth/**",           // trang HTML
                                "/api/auth/**"        // ← API login, register, forgot... (đã thêm lại)
                        ).permitAll()

                        // ── 3. Static resources ────────────────────────────────────────
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // ── 4. Trang sau đăng nhập (JS tự check token) ────────────────
                        .requestMatchers(
                                "/dashboard", "/dashboard/**",
                                "/resident", "/resident/**",
                                "/profile", "/profile/**"
                        ).permitAll()

                        // ── 5. STAFF API ───────────────────────────────────────────────
                        .requestMatchers("/api/staff-management/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/resident-management/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/invoice-management/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/staff/requests/**").hasAnyRole("ADMIN", "MANAGER", "STAFF", "TECHNICIAN")
                        .requestMatchers("/api/staff/vehicles/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/staff/notifications/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/buildings/**", "/api/apartments/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")

                        // ── 6. RESIDENT API ────────────────────────────────────────────
                        .requestMatchers("/api/resident/**").hasAnyRole("OWNER", "TENANT", "GUEST")

                        // ── 7. PROFILE & MOMO ──────────────────────────────────────────
                        .requestMatchers("/api/profile/**").authenticated()
                        .requestMatchers("/api/momo/ipn", "/api/momo/return").permitAll()
                        .requestMatchers("/api/momo/**").hasAnyRole("OWNER", "TENANT", "GUEST")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}