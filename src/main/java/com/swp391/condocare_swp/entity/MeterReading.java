package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_reading")
@Data
public class MeterReading {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type")
    private MeterType meterType;  // ELECTRIC, WATER

    private Integer month;
    private Integer year;

    @Column(name = "previous_index")
    private BigDecimal previousIndex;

    @Column(name = "current_index")
    private BigDecimal currentIndex;

    // DB dùng float → map Double (tránh precision issue với primitive float)
    @Column(name = "total_amount")
    private Double totalAmount;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private Staff recordedBy;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    // ── Computed helpers (không map DB column) ───────────
    /** Tiêu thụ = chỉ số hiện tại - chỉ số trước */
    @Transient
    public BigDecimal getConsumption() {
        if (currentIndex != null && previousIndex != null) {
            return currentIndex.subtract(previousIndex);
        }
        return BigDecimal.ZERO;
    }

    /** totalAmount dưới dạng BigDecimal để Service tính toán */
    @Transient
    public BigDecimal getTotalAmountDecimal() {
        return totalAmount != null ? BigDecimal.valueOf(totalAmount) : BigDecimal.ZERO;
    }

    public enum MeterType {
        ELECTRIC, WATER
    }
}