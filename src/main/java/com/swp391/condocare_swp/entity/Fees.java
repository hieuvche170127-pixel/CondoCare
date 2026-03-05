package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Fees")
@Data
public class Fees {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private FeeType type;  // SERVICE, PARKING, MANAGEMENT, OTHER

    private BigDecimal amount;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    @ManyToOne
    @JoinColumn(name = "create_by")
    private Staff createdBy;

    public enum FeeType {
        SERVICE, PARKING, MANAGEMENT, OTHER
    }
}