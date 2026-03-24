package com.swp391.condocare_swp.security;

import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// Đã bỏ @Component vì bạn đã khai báo @Bean ở SecurityConfig
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Khai báo final để bắt buộc khởi tạo qua constructor
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // THÊM CONSTRUCTOR NÀY ĐỂ KHỚP VỚI SECURITY CONFIG
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Lấy jwt từ request
            String jwt = getJwtFromRequest(request);

            // Kiểm tra token có hợp lệ không
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Lấy username (hoặc email) từ chuỗi jwt
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // Tạo đối tượng Authentication và set vào SecurityContext
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    // Lưu thêm các chi tiết của request (như IP, Session ID nếu có)
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Nạp vào SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Không thể thiết lập xác thực người dùng trong security context", ex);
        }

        // Cho phép request đi tiếp
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Cố gắng lấy từ Header trước (Dành cho API / fetch / axios)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Nếu Header không có, thử lấy từ Cookie (Dành cho Spring MVC duyệt web thông thường)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}