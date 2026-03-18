package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ResidentUpdateRequest {

    private String fullName;

    @Pattern(regexp = "^(OWNER|TENANT|GUEST)$", message = "Type không hợp lệ")
    private String type;

    private LocalDate dob;

    @Pattern(regexp = "^[MF]$", message = "Giới tính phải M hoặc F")
    private String gender;

    @Size(max = 12)
    private String idNumber;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "SĐT phải 10-11 chữ số")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status không hợp lệ")
    private String status;

    private String apartmentId; // null = không đổi, "" = bỏ apartment

    @Size(min = 6, message = "Mật khẩu mới phải ít nhất 6 ký tự")
    private String newPassword;

    private String tempResidence;
    private String tempAbsence;
}