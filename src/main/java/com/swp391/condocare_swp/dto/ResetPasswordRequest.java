package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request reset password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    
    /**
     * Token reset password (nhận từ email)
     */
    @NotBlank(message = "Token không được để trống")
    private String token;
    
    /**
     * Password mới
     */
    @NotBlank(message = "Password mới không được để trống")
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String newPassword;
    
    /**
     * Confirm password mới
     */
    @NotBlank(message = "Confirm password không được để trống")
    private String confirmPassword;
}
