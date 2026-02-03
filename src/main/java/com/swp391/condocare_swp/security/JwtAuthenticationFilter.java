package com.swp391.condocare_swp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter để xử lý JWT authentication cho mỗi request
 * Kế thừa OncePerRequestFilter để đảm bảo filter chỉ chạy 1 lần cho mỗi request
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    /**
     * Method chính để filter request
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Lấy JWT token từ request
            String jwt = getJwtFromRequest(request);
            
            // Nếu có token và token hợp lệ
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Lấy username từ token
                String username = tokenProvider.getUsernameFromToken(jwt);
                
                // Load user details từ username
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                
                // Tạo Authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                
                // Set thêm thông tin request vào authentication
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication vào SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                logger.debug("Set Authentication for user: {}", username);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
        
        // Tiếp tục filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Lấy JWT token từ Authorization header
     * Format: Bearer <token>
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Kiểm tra xem header Authorization có chứa Bearer token không
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Trả về token (bỏ "Bearer " prefix)
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
