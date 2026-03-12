package com.swp391.condocare_swp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    // ── Chung ─────────────────────────────────────────────────
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String dob;       // "yyyy-MM-dd"
    private String gender;    // "M" | "F"
    private String userType;  // "staff" | "resident"

    // ── Staff only ────────────────────────────────────────────
    private String position;
    private String department;
    private String staffStatus;
    private String roleName;

    // ── Resident only ─────────────────────────────────────────
    private String idNumber;
    private String residentType;    // OWNER | TENANT | GUEST
    private String residentStatus;  // ACTIVE | INACTIVE
    private String tempResidence;
    private String tempAbsence;

    // ── Apartment (Resident) ──────────────────────────────────
    private String apartmentId;
    private String apartmentNumber;
    private String buildingName;
    private Double apartmentArea;

    // ── Fees của căn hộ (Resident) ────────────────────────────
    private List<FeeInfo> fees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeeInfo {
        private String  id;
        private String  name;
        private String  type;        // SERVICE | PARKING | MANAGEMENT | OTHER
        private String  vehicleType; // "motorbike" | "car" | "ebike" | null
        private double  amount;
        private boolean active;      // true = effectiveTo IS NULL (đang dùng)
    }
}