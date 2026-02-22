package com.swp391.condocare_swp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho profile response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String position;      // Cho Staff
    private String department;    // Cho Staff
    private String type;          // Cho Resident
    private String apartmentId;   // Cho Resident
    private String userType;      // "staff" hoặc "resident"
}
