package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity đại diện cho bảng Apartment trong database
 */
@Entity
@Table(name = "Apartment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Apartment {
    
    /**
     * ID của Apartment (Primary Key)
     */
    @Id
    @Column(name = "ID", length = 20, nullable = false)
    private String id;
    
    /**
     * Số căn hộ (VD: A101, B205)
     */
    @Column(name = "number", length = 4, nullable = false)
    private String number;
    
    /**
     * Tầng của căn hộ
     */
    @Column(name = "floor", nullable = false)
    private Integer floor;
    
    /**
     * Diện tích (m²)
     */
    @Column(name = "area", nullable = false)
    private Float area;

    @Column(name = "sale_price")
    private Float salePrice;

    @Column(name = "rent_price")
    private Float rentPrice;

    /**
     * Tòa nhà chứa căn hộ
     * Many-to-One: Một Building có nhiều Apartment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    /**
     * Trạng thái căn hộ: EMPTY, OCCUPIED, MAINTENANCE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('EMPTY', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'EMPTY'")
    private ApartmentStatus status = ApartmentStatus.EMPTY;
    
    /**
     * Trạng thái cho thuê: AVAILABLE, RENTED, OWNER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rental_status", columnDefinition = "ENUM('AVAILABLE', 'RENTED', 'OWNER') DEFAULT 'AVAILABLE'")
    private RentalStatus rentalStatus = RentalStatus.AVAILABLE;
    
    /**
     * Đường dẫn ảnh căn hộ (có thể nhiều ảnh, phân cách bằng dấu phẩy)
     */
    @Column(name = "images", length = 255)
    private String images;
    
    /**
     * Mô tả về căn hộ
     */
    @Column(name = "description", length = 255)
    private String description;
    
    /**
     * Tổng số cư dân đang ở
     */
    @Column(name = "total_resident")
    private Integer totalResident = 0;
    
    /**
     * Tổng số phương tiện đã đăng ký
     */
    @Column(name = "total_vehicle")
    private Integer totalVehicle = 0;

    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.isEmpty()) {
            String bId = (this.building != null && this.building.getId() != null) ? this.building.getId() : "UNK";
            this.id = bId + this.floor + this.number;
        }
        if (status == null) status = ApartmentStatus.EMPTY;
        if (rentalStatus == null) rentalStatus = RentalStatus.AVAILABLE;
    }

    /**
     * Enum cho Apartment Status
     */
    public enum ApartmentStatus {
        EMPTY,        // Trống
        OCCUPIED,     // Đang ở
        MAINTENANCE   // Đang bảo trì
    }
    
    /**
     * Enum cho Rental Status
     */
    public enum RentalStatus {
        AVAILABLE,    // Có thể thuê
        RENTED,       // Đang cho thuê
        OWNER         // Chủ sở hữu đang ở
    }
}
