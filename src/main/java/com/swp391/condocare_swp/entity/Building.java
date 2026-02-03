package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity đại diện cho bảng Building trong database
 * Quản lý bởi Staff có Role Manager
 */
@Entity
@Table(name = "Building")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Building {
    
    /**
     * ID của Building (Primary Key)
     */
    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;
    
    /**
     * Tên tòa nhà
     */
    @Column(name = "name", length = 255, nullable = false)
    private String name;
    
    /**
     * Địa chỉ tòa nhà
     */
    @Column(name = "address", length = 255, nullable = false)
    private String address;
    
    /**
     * Tổng số tầng
     */
    @Column(name = "total_floors", nullable = false)
    private Integer totalFloors;
    
    /**
     * Tổng số căn hộ
     */
    @Column(name = "total_apartments", nullable = false)
    private Integer totalApartments;
    
    /**
     * Manager quản lý tòa nhà
     * Many-to-One: Một Manager có thể quản lý nhiều Building
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Staff manager;
}
