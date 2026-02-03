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
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration class cho Spring Security
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Cho phép dùng @PreAuthorize, @Secured...
public class SecurityConfig {

    /**
     * Bean để mã hóa password
     * Sử dụng BCrypt - thuật toán mã hóa một chiều an toàn
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * Bean để tạo JWT Authentication Filter
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * Bean AuthenticationManager
     * Dùng để authenticate user
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Cấu hình Security Filter Chain
     * Định nghĩa các rule authorization và authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF vì dùng JWT (stateless)
                .csrf(csrf -> csrf.disable())

                // Cấu hình session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Cấu hình CORS
                .cors(cors -> cors.disable())

                // Cấu hình authorization
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập public (không cần đăng nhập)
                        .requestMatchers(
                                "/",
                                "/api/auth/**",
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Dashboard chỉ cho Staff
                        .requestMatchers("/dashboard/**", "/api/dashboard/**").permitAll()

                        // Tất cả request khác cần authenticate
                        .anyRequest().authenticated()
                )

                // Disable form login và http basic
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Thêm JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
