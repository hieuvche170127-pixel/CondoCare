package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
    @Id
    @Column(length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Residents resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType type;

    @Column(name = "total_value", nullable = false)
    private Float totalValue;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('DRAFT', 'PENDING', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'REJECT') DEFAULT 'DRAFT'")
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = ContractStatus.DRAFT;
    }

    public enum ContractType { SALE, RENT }

    public enum ContractStatus {
        DRAFT, PENDING, ACTIVE, EXPIRED, TERMINATED, REJECT
    }
}