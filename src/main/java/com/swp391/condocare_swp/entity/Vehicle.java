package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Vehicle")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false,
            columnDefinition = "ENUM('MOTORBIKE','CAR','BICYCLE','ELECTRIC_BIKE','OTHER')")
    private VehicleType type;

    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "color", length = 50)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Residents resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false,
            columnDefinition = "ENUM('MONTHLY','QUARTERLY','YEARLY')")
    private DurationType durationType = DurationType.MONTHLY;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /**
     * PENDING  = Resident vừa đăng ký, chờ Staff duyệt
     * APPROVED = Đã được duyệt
     * REJECTED = Bị từ chối
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pending_status", nullable = false,
            columnDefinition = "ENUM('PENDING','APPROVED','REJECTED')")
    private PendingStatus pendingStatus = PendingStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Staff approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    /**
     * ACTIVE   = Đang gửi xe hợp lệ
     * INACTIVE = Tạm ngừng
     * LOST     = Mất thẻ / không tìm thấy xe
     * REVOKED  = Bị thu hồi đăng ký
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('ACTIVE','INACTIVE','LOST','REVOKED')")
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (pendingStatus == null) pendingStatus = PendingStatus.PENDING;
        if (status        == null) status        = VehicleStatus.ACTIVE;
        if (durationType  == null) durationType  = DurationType.MONTHLY;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Enums ────────────────────────────────────────────────

    public enum VehicleType {
        MOTORBIKE, CAR, BICYCLE, ELECTRIC_BIKE, OTHER
    }

    public enum DurationType {
        MONTHLY,    // Theo tháng
        QUARTERLY,  // Theo quý (3 tháng)
        YEARLY      // Theo năm
    }

    public enum PendingStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum VehicleStatus {
        ACTIVE, INACTIVE, LOST, REVOKED
    }
}