package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Invoice",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_invoice_apt_month",
                columnNames = {"apartment_id", "month", "year"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @Column(name = "ID", length = 15, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    /** 1–12 */
    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('UNPAID','PAID','OVERDUE')")
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Staff createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Chi tiết từng dòng phí — cascade delete khi xóa invoice */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceFeeDetail> feeDetails;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status      == null) status      = InvoiceStatus.UNPAID;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
    }

    public enum InvoiceStatus { UNPAID, PAID, OVERDUE }
}