package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request đăng nhập
 * Chấp nhận cả username và email
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    /**
     * Username hoặc Email để đăng nhập
     * @NotBlank: Không được để trống
     */
    @NotBlank(message = "Username hoặc Email không được để trống")
    private String usernameOrEmail;
    
    /**
     * Password
     */
    @NotBlank(message = "Password không được để trống")
    private String password;
    
    /**
     * Loại user: "staff" hoặc "resident"
     */
    @NotBlank(message = "User type không được để trống")
    private String userType; // "staff" or "resident"
}
