package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AccessCard {
    @Id
    @Column(length = 10, nullable = false)
    private String id;

    @Column(name = "card_number", length = 8, nullable = false, unique = true)
    private String cardNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private Staff issuedBy;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('ACTIVE', 'BLOCKED', 'LOST') DEFAULT 'ACTIVE'")
    private CardStatus status = CardStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Residents resident;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
        if (status == null) status = CardStatus.ACTIVE;
    }

    public enum CardStatus { ACTIVE, BLOCKED, LOST }
}