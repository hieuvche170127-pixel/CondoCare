package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Vehicle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Vehicle {
    @Id
    @Column(length = 10, nullable = false)
    private String id;

    @Column(length = 100, nullable = false)
    private String type;

    @Column(name = "license_plate", length = 100, unique = true)
    private String licensePlate;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'")
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Residents resident;

    @PrePersist
    protected void onCreate() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
        if (status == null) status = VehicleStatus.ACTIVE;
    }

    public enum VehicleStatus { ACTIVE, INACTIVE }
}