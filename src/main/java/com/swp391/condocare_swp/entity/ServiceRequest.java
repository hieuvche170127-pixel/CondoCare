package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity bảng ServiceRequest
 * Yêu cầu hỗ trợ / báo sự cố từ cư dân gửi lên Ban quản lý
 */
@Entity
@Table(name = "ServiceRequest")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @Column(name = "ID", length = 15, nullable = false)
    private String id;

    /** Tiêu đề ngắn mô tả vấn đề */
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /** Mô tả chi tiết */
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    /** Danh mục: điện, nước, internet, ... */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category = Category.OTHER;

    /** Trạng thái xử lý */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    /** Mức độ ưu tiên */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;

    /** Cư dân gửi yêu cầu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Residents resident;

    /** Căn hộ liên quan */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = true)
    private Apartment apartment;

    /** Staff được giao xử lý (null = chưa phân công) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = true)
    private Staff assignedTo;

    /** Ghi chú của Staff khi xử lý */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Category {
        ELECTRIC,   // Điện
        WATER,      // Nước
        INTERNET,   // Internet
        HVAC,       // Điều hòa / thông gió
        STRUCTURE,  // Kết cấu tòa nhà
        OTHER       // Khác
    }

    public enum RequestStatus {
        PENDING,     // Chờ xử lý
        IN_PROGRESS, // Đang xử lý
        DONE,        // Hoàn thành
        REJECTED     // Từ chối
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }
}
