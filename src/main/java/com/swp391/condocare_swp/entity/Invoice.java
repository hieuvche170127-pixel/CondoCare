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
@Table(name = "Invoice_Monthly",
       uniqueConstraints = @UniqueConstraint(columnNames = {"apartment_id","month","year"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @Column(name = "ID", length = 15, nullable = false)
    private String id;

    /** Căn hộ được lập hóa đơn */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    /** Tháng (1-12) */
    @Column(name = "month", nullable = false)
    private Integer month;

    /** Năm */
    @Column(name = "year", nullable = false)
    private Integer year;

    /** Số kWh điện tiêu thụ */
    @Column(name = "electric_kwh")
    private Float electricKwh = 0f;

    /** Số m³ nước tiêu thụ */
    @Column(name = "water_m3")
    private Float waterM3 = 0f;

    /** Tiền điện */
    @Column(name = "electric_amt", precision = 15, scale = 2)
    private BigDecimal electricAmt = BigDecimal.ZERO;

    /** Tiền nước */
    @Column(name = "water_amt", precision = 15, scale = 2)
    private BigDecimal waterAmt = BigDecimal.ZERO;

    /** Phí dịch vụ / quản lý */
    @Column(name = "service_amt", precision = 15, scale = 2)
    private BigDecimal serviceAmt = BigDecimal.ZERO;

    /** Phí gửi xe */
    @Column(name = "parking_amt", precision = 15, scale = 2)
    private BigDecimal parkingAmt = BigDecimal.ZERO;

    /** Tổng cộng */
    @Column(name = "total_amt", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmt = BigDecimal.ZERO;

    /** Trạng thái thanh toán */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    /** Hạn thanh toán */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Thời điểm thanh toán thực tế */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Staff tạo hóa đơn */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Staff createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum InvoiceStatus {
        UNPAID,  // Chưa thanh toán
        PAID,    // Đã thanh toán
        OVERDUE  // Quá hạn
    }
}
