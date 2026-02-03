package com.swp391.condocare_swp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity đại diện cho bảng Role trong database
 * Dùng để phân quyền cho Staff (Admin, Manager, Staff...)
 */
@Entity
@Table(name = "Role")
@Data // Lombok tự động tạo getter, setter, toString, equals, hashCode
@NoArgsConstructor // Constructor không tham số
@AllArgsConstructor // Constructor với tất cả tham số
public class Role {
    
    /**
     * ID của Role (Primary Key)
     * Độ dài tối đa: 10 ký tự
     */
    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;
    
    /**
     * Tên của Role
     * Ví dụ: ADMIN, MANAGER, STAFF
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    
    /**
     * Mô tả về Role
     */
    @Column(name = "description", length = 255)
    private String description;
}
