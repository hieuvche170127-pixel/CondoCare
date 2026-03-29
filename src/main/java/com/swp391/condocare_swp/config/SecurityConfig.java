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

/**
 * SecurityConfig — phân quyền URL-level.
 *
 * Các role thực tế trong DB: ADMIN, MANAGER, ACCOUNTANT, TECHNICIAN, RECEPTIONIST
 * Phân quyền chi tiết từng method được xử lý thêm bằng @PreAuthorize ở controller.
 *
 * Tóm tắt phân quyền:
 *   ADMIN         — toàn quyền
 *   MANAGER       — toàn quyền trên tòa nhà mình quản lý (giống ADMIN trên UI)
 *   ACCOUNTANT    — xem danh sách hóa đơn, căn hộ, dashboard; KHÔNG tạo/xóa
 *   TECHNICIAN    — xem yêu cầu được phân công, đánh dấu hoàn thành
 *   RECEPTIONIST  — phân công kỹ thuật viên, gửi thông báo, xem danh sách
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // ── Tất cả role nhân viên để gọi tắt ──────────────────────────────────────
    private static final String[] ALL_STAFF =
            {"ADMIN", "MANAGER", "ACCOUNTANT", "TECHNICIAN", "RECEPTIONIST"};

    // ── Role có quyền quản lý (admin + manager) ───────────────────────────────
    private static final String[] MGMT_ROLES = {"ADMIN", "MANAGER"};

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

                        // ── 1. CORS preflight ──────────────────────────────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── 2. Public — auth pages + API ──────────────────────────────
                        .requestMatchers(
                                "/", "/login", "/auth/login",
                                "/register", "/auth/register",
                                "/forgot-password", "/auth/forgot-password",
                                "/reset-password", "/auth/reset-password",
                                "/auth/**",
                                "/api/auth/**"
                        ).permitAll()

                        // ── 3. Static resources ────────────────────────────────────────
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // ── 4. Trang HTML (JS tự kiểm tra token) ─────────────────────
                        .requestMatchers(
                                "/dashboard", "/dashboard/**",
                                "/resident", "/resident/**",
                                "/profile", "/profile/**"
                        ).permitAll()

                        // ── 5. MoMo callback (không có JWT) ───────────────────────────
                        .requestMatchers("/api/momo/ipn", "/api/momo/return").permitAll()

                        // ── 6. Quản lý nhân viên — chỉ ADMIN + MANAGER ──────────────
                        .requestMatchers("/api/staff-management/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // ── 7. Quản lý cư dân — ADMIN + MANAGER ─────────────────────
                        .requestMatchers("/api/resident-management/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // ── 8. Hóa đơn ───────────────────────────────────────────────
                        //   GET  → ADMIN, MANAGER, ACCOUNTANT (xem danh sách)
                        //   POST/PATCH/DELETE → chỉ ADMIN, MANAGER (tạo/sửa/xóa)
                        //   Chi tiết hơn được xử lý bằng @PreAuthorize ở controller
                        .requestMatchers(HttpMethod.GET, "/api/invoice-management/**")
                        .hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/api/invoice-management/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/invoice-management/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/invoice-management/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // ── 9. Yêu cầu hỗ trợ ────────────────────────────────────────
                        //   Tất cả nhân viên đều vào được (filter chi tiết ở service/controller)
                        .requestMatchers("/api/staff/requests/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN", "RECEPTIONIST", "ACCOUNTANT")

                        // ── 10. Phương tiện ───────────────────────────────────────────
                        .requestMatchers("/api/staff/vehicles/**")
                        .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                        // ── 11. Thông báo ─────────────────────────────────────────────
                        .requestMatchers("/api/staff/notifications/**")
                        .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                        // ── 12. Tòa nhà / Căn hộ ─────────────────────────────────────
                        .requestMatchers("/api/buildings/**", "/api/apartments/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // ── 13. Dashboard ─────────────────────────────────────────────
                        .requestMatchers("/api/dashboard/**")
                        .hasAnyRole(ALL_STAFF)

                        // ── 14. Profile ───────────────────────────────────────────────
                        .requestMatchers("/api/profile/**").authenticated()

                        // ── 15. MoMo (resident thanh toán) ───────────────────────────
                        .requestMatchers("/api/momo/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")

                        // ── 16. Resident API ──────────────────────────────────────────
                        .requestMatchers("/api/resident/**")
                        .hasAnyRole("OWNER", "TENANT", "GUEST")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}