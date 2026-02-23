package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity bảng Notification
 * Thông báo từ Ban quản lý (Staff) gửi tới cư dân.
 * - resident_id = NULL  → Thông báo chung cho TẤT CẢ cư dân
 * - resident_id có giá trị → Thông báo cá nhân cho 1 cư dân cụ thể
 */
@Entity
@Table(name = "Notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @Column(name = "ID", length = 15, nullable = false)
    private String id;

    /** Tiêu đề ngắn */
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /** Nội dung chi tiết */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Loại thông báo */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type = NotificationType.INFO;

    /**
     * Cư dân nhận thông báo (NULL = gửi tất cả)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = true)
    private Residents resident;

    /** Trạng thái đã đọc hay chưa */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /** Staff tạo thông báo */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Staff createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        INFO,         // Thông tin chung
        WARNING,      // Cảnh báo
        URGENT,       // Khẩn cấp
        MAINTENANCE,  // Bảo trì
        PAYMENT       // Nhắc thanh toán
    }
}
