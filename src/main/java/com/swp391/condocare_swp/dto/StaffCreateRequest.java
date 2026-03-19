package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StaffCreateRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;

    // Không bắt buộc — nếu null/blank thì service tự sinh và gửi qua email
    @Size(min = 6, message = "Password phải ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải 10-11 chữ số")
    private String phone;

    private String position;     // Vị trí
    private String department;   // Phòng ban
    private LocalDate dob;       // Ngày sinh

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^[MF]$", message = "Giới tính phải là M hoặc F")
    private String gender;

    @NotBlank(message = "Role ID không được để trống")
    private String roleId;       // R001, R002, R003

    private LocalDateTime hiredAt; // Ngày vào làm (optional, default = now)
}