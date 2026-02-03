package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request đăng ký resident mới
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    /**
     * Username
     */
    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50, message = "Username phải từ 4-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username chỉ chứa chữ cái, số và dấu gạch dưới")
    private String username;
    
    /**
     * Password
     */
    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String password;
    
    /**
     * Confirm Password
     */
    @NotBlank(message = "Confirm password không được để trống")
    private String confirmPassword;
    
    /**
     * Họ tên đầy đủ
     */
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    /**
     * Email
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    /**
     * Số điện thoại
     */
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;
    
    /**
     * Số CMND/CCCD
     */
    @NotBlank(message = "Số CMND/CCCD không được để trống")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Số CMND/CCCD phải có 9-12 chữ số")
    private String idNumber;
    
    /**
     * ID căn hộ
     */
    @NotBlank(message = "ID căn hộ không được để trống")
    private String apartmentId;
    
    /**
     * Loại cư dân: OWNER, TENANT, GUEST
     */
    @NotBlank(message = "Loại cư dân không được để trống")
    private String residentType;
    
    /**
     * Giới tính: M hoặc F
     */
    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^[MF]$", message = "Giới tính phải là M hoặc F")
    private String gender;
}
