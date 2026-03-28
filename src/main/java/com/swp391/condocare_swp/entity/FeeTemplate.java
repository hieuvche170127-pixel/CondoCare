package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "FeeTemplate")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeeTemplate {

    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false,
            columnDefinition = "ENUM('SERVICE','PARKING')")
    private FeeType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * PER_APT = tính theo căn hộ (flat fee mỗi căn)
     * PER_M2  = tính theo diện tích m²
     * FIXED   = cố định, không phụ thuộc căn hộ
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false,
            columnDefinition = "ENUM('PER_APT','PER_M2','FIXED')")
    private FeeUnit unit = FeeUnit.FIXED;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** NULL = vẫn còn hiệu lực */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('ACTIVE','INACTIVE')")
    private FeeStatus status = FeeStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Staff createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "min_area")
    private BigDecimal minArea; // null = không giới hạn dưới

    @Column(name = "max_area")
    private BigDecimal maxArea; // null = không giới hạn trên

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = FeeStatus.ACTIVE;
        if (unit   == null) unit   = FeeUnit.FIXED;
    }

    public enum FeeType   { SERVICE, PARKING }
    public enum FeeUnit   { PER_APT, PER_M2, FIXED }
    public enum FeeStatus { ACTIVE, INACTIVE }
}