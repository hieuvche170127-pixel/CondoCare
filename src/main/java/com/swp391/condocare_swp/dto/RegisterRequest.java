package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống.")
    @Size(min = 4, max = 50, message = "Username phải từ 4–50 ký tự.")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$",
            message = "Username chỉ chứa chữ cái, số, dấu chấm và dấu gạch dưới.")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự.")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống.")
    private String confirmPassword;

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10–11 chữ số.")
    private String phone;

    /**
     * Số CCCD — optional.
     * Manager dùng để đối chiếu xác minh danh tính trước khi duyệt PENDING → ACTIVE.
     */
    @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải gồm đúng 12 chữ số.")
    private String idNumber;
}