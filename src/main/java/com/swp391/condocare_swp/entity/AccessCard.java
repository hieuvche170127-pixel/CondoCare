package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_cards")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessCard {

    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;

    @Column(name = "card_number", length = 20, nullable = false, unique = true)
    private String cardNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Residents resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private Staff issuedBy;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('ACTIVE','BLOCKED','LOST')")
    private CardStatus status = CardStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = CardStatus.ACTIVE;
    }

    public enum CardStatus {
        ACTIVE,   // Đang hoạt động
        BLOCKED,  // Bị khóa
        LOST      // Đã mất
    }
}