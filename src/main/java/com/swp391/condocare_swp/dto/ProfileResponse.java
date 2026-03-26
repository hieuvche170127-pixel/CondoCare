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
    private String staffStatus;   // ACTIVE | RESIGNED | ON_LEAVE  (Staff.StaffStatus)
    private String roleName;

    // ── Resident only ─────────────────────────────────────────
    private String idNumber;
    private String residentType;   // OWNER | TENANT | GUEST       (Residents.ResidentType)
    private String residentStatus; // PENDING | ACTIVE | INACTIVE  (Residents.ResidentStatus)

    // ── Apartment ─────────────────────────────────────────────
    private String apartmentId;
    private String apartmentNumber;
    private String buildingName;
    private Double apartmentArea;

    // ── Thẻ từ (hiện sau khi ACTIVE) ─────────────────────────
    private String accessCardNumber;
    private String accessCardStatus; // ACTIVE | BLOCKED | LOST    (AccessCard.CardStatus)

    // ── Xe đã đăng ký ────────────────────────────────────────
    private List<VehicleInfo> vehicles;

    // ── Phí dịch vụ tòa nhà (để resident xem) ────────────────
    private List<FeeTemplateInfo> feeTemplates;

    // ═════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleInfo {
        private String id;
        private String type;          // Vehicle.VehicleType: MOTORBIKE|CAR|BICYCLE|ELECTRIC_BIKE|OTHER
        private String licensePlate;
        private String brand;
        private String model;
        private String color;
        private String durationType;  // MONTHLY | QUARTERLY | YEARLY
        private String registeredAt;
        private String expiredAt;
        private String pendingStatus; // PENDING | APPROVED | REJECTED
        private String status;        // ACTIVE | INACTIVE | LOST | REVOKED
        private String rejectReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeeTemplateInfo {
        private String id;
        private String name;
        private String type;          // SERVICE | PARKING
        private String unit;          // PER_APT | PER_M2 | FIXED
        private double amount;
        private String effectiveFrom; // "yyyy-MM-dd"
    }
}