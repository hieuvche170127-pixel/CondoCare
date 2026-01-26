package com.swp391.condocare_swp.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class UserDto {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String status;

    // lấy từ user.getRole().getId()
    private String roleId;

    private Instant createAt;
    private Instant lastLogin;
}
