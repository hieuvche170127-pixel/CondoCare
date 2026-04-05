package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity cho bảng Payments — lưu lịch sử thanh toán hóa đơn.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "Payments")
public class Payments {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    /** FK → Invoice.ID (varchar 15) */
    @Column(name = "invoice_id", length = 15, nullable = false)
    private String invoiceId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    /**
     * Phương thức thanh toán — khớp ENUM DB: CASH | BANKING | MOMO | ZALOPAY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false,
            columnDefinition = "ENUM('CASH','BANKING','MOMO','ZALOPAY')")
    private PaymentMethod method;

    /**
     * MoMo transId từ IPN callback.
     * MoMo có thể trả transId tối đa 50 ký tự — khớp varchar(50) trong DB.
     */
    @Column(name = "momo_trans_id", length = 50)
    private String momoTransId;

    /** orderId gửi lên MoMo (= invoiceId + "_" + timestamp) */
    @Column(name = "momo_order_id", length = 50)
    private String momoOrderId;

    /** Ghi chú thanh toán — khớp varchar(255) trong DB */
    @Column(name = "note", length = 255)
    private String note;

    /** FK → Residents.ID */
    @Column(name = "paid_by", length = 10, nullable = false)
    private String paidBy;

    // ── Enum phương thức thanh toán ──────────────────────────────────────────

    public enum PaymentMethod {
        CASH,       // Tiền mặt
        BANKING,    // Chuyển khoản ngân hàng
        MOMO,       // Ví MoMo
        ZALOPAY     // Ví ZaloPay
    }
}