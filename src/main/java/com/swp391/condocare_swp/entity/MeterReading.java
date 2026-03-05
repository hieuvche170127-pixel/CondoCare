package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MeterReading")
@Data
public class MeterReading {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    private MeterType meterType;  // ELECTRIC, WATER

    private Integer month;
    private Integer year;
    private BigDecimal previousIndex;
    private BigDecimal currentIndex;
    private BigDecimal consumption;  // current - previous
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;  // consumption * unitPrice

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private Staff recordedBy;

    private LocalDateTime recordedAt;

    public enum MeterType {
        ELECTRIC, WATER
    }
}
