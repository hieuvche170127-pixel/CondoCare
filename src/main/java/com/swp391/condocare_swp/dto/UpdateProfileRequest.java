package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

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

    private String dob;     // "yyyy-MM-dd" — optional
    private String gender;  // "M" | "F"   — optional

    // ── Resident only ─────────────────────────────────────────
    private String idNumber;
    private String tempResidence;
    private String tempAbsence;

    /**
     * Danh sách feeId mà resident MUỐN SỬ DỤNG (checkbox đã tick).
     * - Có trong list + đang tắt  → bật lại  (set effectiveTo = null)
     * - Không có trong list + đang bật → tắt (set effectiveTo = today)
     * - null → không thay đổi fee nào
     */
    private List<String> selectedFeeIds;
}