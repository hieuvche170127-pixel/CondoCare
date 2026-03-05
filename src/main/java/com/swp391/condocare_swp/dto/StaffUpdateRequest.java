package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class StaffUpdateRequest {

    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "SĐT phải 10-11 chữ số")
    private String phone;

    private String position;
    private String department;
    private LocalDate dob;

    @Pattern(regexp = "^[MF]$", message = "Giới tính phải M hoặc F")
    private String gender;

    private String roleId;

    @Pattern(regexp = "^(ACTIVE|RESIGNED|ON_LEAVE)$", message = "Status không hợp lệ")
    private String status;

    @Size(min = 6, message = "Mật khẩu mới phải ít nhất 6 ký tự")
    private String newPassword; // null = không đổi password
}