package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    // ── Chung ─────────────────────────────────────────────────
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;

    private String dob;    // "yyyy-MM-dd" — optional
    private String gender; // "M" | "F"   — optional

    // ── Resident only ─────────────────────────────────────────
    // Chỉ cho phép cập nhật khi status = PENDING (chưa xác minh)
    private String idNumber;

    // ĐÃ XÓA so với version cũ:
    // - selectedFeeIds  → đăng ký xe dùng API riêng: POST /api/vehicles
    // - tempResidence   → không còn trong schema mới (Residents.java)
    // - tempAbsence     → không còn trong schema mới (Residents.java)
}