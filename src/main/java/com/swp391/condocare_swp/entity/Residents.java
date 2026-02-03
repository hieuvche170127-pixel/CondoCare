package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng Residents trong database
 * Gộp thông tin Account và Resident
 */
@Entity
@Table(name = "Residents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Residents {
    
    /**
     * ID của Resident (Primary Key)
     */
    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;
    
    /**
     * Username để đăng nhập
     */
    @Column(name = "username", length = 255, nullable = false, unique = true)
    private String username;
    
    /**
     * Password đã mã hóa
     */
    @Column(name = "password", length = 255, nullable = false)
    private String password;
    
    /**
     * Họ và tên đầy đủ
     */
    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;
    
    /**
     * Loại cư dân: OWNER, TENANT, GUEST
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "ENUM('OWNER', 'TENANT', 'GUEST')")
    private ResidentType type;
    
    /**
     * Ngày sinh
     */
    @Column(name = "dob")
    private LocalDate dob;
    
    /**
     * Giới tính
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, columnDefinition = "ENUM('M', 'F')")
    private Gender gender;
    
    /**
     * Số CMND/CCCD
     */
    @Column(name = "id_number", length = 12, nullable = false)
    private String idNumber;
    
    /**
     * Số điện thoại
     */
    @Column(name = "phone", length = 20, nullable = false)
    private String phone;
    
    /**
     * Email
     */
    @Column(name = "email", length = 255)
    private String email;
    
    /**
     * Trạng thái: ACTIVE, INACTIVE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('ACTIVE', 'INACTIVE')")
    private ResidentStatus status = ResidentStatus.ACTIVE;
    
    /**
     * Căn hộ của cư dân
     * Many-to-One: Một Apartment có nhiều Residents
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;
    
    /**
     * Địa chỉ tạm trú
     */
    @Column(name = "temp_residence", length = 255)
    private String tempResidence;
    
    /**
     * Thông tin tạm vắng
     */
    @Column(name = "temp_absence", length = 255)
    private String tempAbsence;
    
    /**
     * Lần đăng nhập cuối
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    /**
     * Ngày tạo account
     */
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;
    
    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
        if (status == null) {
            status = ResidentStatus.ACTIVE;
        }
    }
    
    /**
     * Enum cho Resident Type
     */
    public enum ResidentType {
        OWNER,   // Chủ sở hữu
        TENANT,  // Người thuê
        GUEST    // Khách
    }
    
    /**
     * Enum cho Gender
     */
    public enum Gender {
        M, // Male
        F  // Female
    }
    
    /**
     * Enum cho Resident Status
     */
    public enum ResidentStatus {
        ACTIVE,    // Đang hoạt động
        INACTIVE   // Không hoạt động
    }
}
