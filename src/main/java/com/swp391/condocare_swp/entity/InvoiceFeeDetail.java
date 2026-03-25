package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_fee_detail")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFeeDetail {

    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * Có thể NULL nếu FeeTemplate đã bị xóa sau khi phát hành hóa đơn.
     * Dữ liệu thực tế đã được snapshot vào fee_name, unit_amount, amount.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_template_id")
    private FeeTemplate feeTemplate;

    /** Snapshot tên phí tại thời điểm phát hành — không bị ảnh hưởng dù template bị sửa */
    @Column(name = "fee_name", length = 255, nullable = false)
    private String feeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false,
            columnDefinition = "ENUM('SERVICE','PARKING')")
    private FeeTemplate.FeeType feeType;

    /** Đơn giá tại thời điểm phát hành */
    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    /** Số lượng: số xe, số m², hoặc 1 nếu FIXED */
    @Column(name = "quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    /** Thành tiền = unit_amount × quantity */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}