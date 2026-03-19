package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ResidentCreateRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;

    // Không bắt buộc — nếu null/blank thì service tự sinh và gửi qua email
    @Size(min = 6, message = "Password phải ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Loại cư dân không được để trống")
    @Pattern(regexp = "^(OWNER|TENANT|GUEST)$", message = "Type phải là OWNER, TENANT hoặc GUEST")
    private String type;

    private LocalDate dob;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^[MF]$", message = "Giới tính phải là M hoặc F")
    private String gender;

    @Size(max = 12, message = "CCCD tối đa 12 ký tự")
    private String idNumber;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "SĐT phải 10-11 chữ số")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String apartmentId; // Có thể null nếu chưa phân căn hộ
}