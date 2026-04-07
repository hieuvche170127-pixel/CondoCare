package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @Column(name = "ID", length = 15, nullable = false)
    private String id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type = NotificationType.INFO;

    /**
     * NULL = broadcast toàn tòa nhà
     * Có giá trị = gửi riêng cho 1 cư dân
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = true)
    private Residents resident;

    /** NULL = gửi toàn tòa nhà */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = true)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = true)
    private Building building;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * NULL = thông báo tự động từ hệ thống (scheduler).
     * Có giá trị = staff đã gửi thủ công.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    private Staff createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
    }

    public enum NotificationType {
        INFO, WARNING, URGENT, MAINTENANCE, PAYMENT
    }
}