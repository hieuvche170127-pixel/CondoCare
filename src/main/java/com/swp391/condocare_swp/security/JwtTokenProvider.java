package com.swp391.condocare_swp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Class xử lý tạo và validate JWT Token
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    /**
     * Secret key để mã hóa JWT (lấy từ application.properties)
     */
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    /**
     * Thời gian hết hạn của JWT (milliseconds)
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    private SecretKey key;
    
    /**
     * Khởi tạo Secret Key sau khi inject properties
     */
    @PostConstruct
    public void init() {
        // Tạo SecretKey từ jwtSecret string
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Tạo JWT token từ Authentication
     * @param authentication Spring Security Authentication
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        // Lấy username từ principal
        String username = authentication.getName();
        
        // Thời gian hiện tại
        Date now = new Date();
        // Thời gian hết hạn
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        // Tạo JWT token
        return Jwts.builder()
                .subject(username)                    // Subject là username
                .issuedAt(now)                        // Thời gian tạo
                .expiration(expiryDate)               // Thời gian hết hạn
                .signWith(key)                        // Ký với secret key
                .compact();                           // Tạo chuỗi JWT
    }
    
    /**
     * Tạo JWT token từ username (dùng cho reset password)
     * @param username Username
     * @return JWT token string
     */
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
    
    /**
     * Lấy username từ JWT token
     * @param token JWT token
     * @return Username
     */
    public String getUsernameFromToken(String token) {
        // Parse JWT và lấy Claims
        Claims claims = Jwts.parser()
                .verifyWith(key)                      // Verify với secret key
                .build()
                .parseSignedClaims(token)             // Parse token
                .getPayload();                        // Lấy payload (claims)
        
        return claims.getSubject();                   // Lấy subject (username)
    }
    
    /**
     * Validate JWT token
     * @param token JWT token
     * @return true nếu token hợp lệ
     */
    public boolean validateToken(String token) {
        try {
            // Parse và verify token
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
