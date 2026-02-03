package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request quên mật khẩu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {
    
    /**
     * Email để gửi link reset password
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    /**
     * Loại user: "staff" hoặc "resident"
     */
    @NotBlank(message = "User type không được để trống")
    private String userType;
}
