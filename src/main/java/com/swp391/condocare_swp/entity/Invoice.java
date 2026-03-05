package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity bảng Invoice_Monthly (để tránh conflict với bảng Invoice gốc)
 * Hóa đơn hàng tháng của từng căn hộ - dành cho Resident Dashboard.
 * Mỗi căn hộ chỉ có duy nhất 1 hóa đơn cho mỗi tháng/năm (unique key).
 */
@Entity
@Table(name = "Invoice")
@Data
public class Invoice {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private Integer month;
    private Integer year;

    // Liên kết với MeterReading
    @ManyToOne
    @JoinColumn(name = "electric_reading_id")
    private MeterReading electricReading;

    @ManyToOne
    @JoinColumn(name = "water_reading_id")
    private MeterReading waterReading;

    // Liên kết với Fees
    @ManyToOne
    @JoinColumn(name = "service_fee_id")
    private Fees serviceFee;

    @ManyToOne
    @JoinColumn(name = "parking_fee_id")
    private Fees parkingFee;

    // Các số tiền
    private BigDecimal electricAmount;
    private BigDecimal waterAmount;
    private BigDecimal serviceAmount;
    private BigDecimal parkingAmount;
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;  // UNPAID, PAID, OVERDUE

    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name = "create_by")
    private Staff createdBy;

    public enum InvoiceStatus {
        UNPAID, PAID, OVERDUE
    }
}