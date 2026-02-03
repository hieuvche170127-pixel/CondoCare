package com.swp391.condocare_swp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho response sau khi đăng nhập thành công
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * Loại token (thường là "Bearer")
     */
    private String type = "Bearer";
    
    /**
     * ID của user
     */
    private String id;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * Email
     */
    private String email;
    
    /**
     * Tên đầy đủ
     */
    private String fullName;
    
    /**
     * Role (cho Staff) hoặc Type (cho Resident)
     */
    private String role;
    
    /**
     * Loại user: "staff" hoặc "resident"
     */
    private String userType;
    
    /**
     * Constructor với token
     */
    public AuthResponse(String token, String id, String username, String email, 
                       String fullName, String role, String userType) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.userType = userType;
    }
}
