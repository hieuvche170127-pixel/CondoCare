package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng Staff trong database
 * Gộp thông tin Account và Staff vào một bảng
 */
@Entity
@Table(name = "Staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Staff {
    
    /**
     * ID của Staff (Primary Key)
     */
    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;
    
    /**
     * Username để đăng nhập
     * Phải unique (không trùng lặp)
     */
    @Column(name = "username", length = 255, nullable = false, unique = true)
    private String username;
    
    /**
     * Password đã được mã hóa (sẽ dùng BCrypt)
     */
    @Column(name = "password", length = 255, nullable = false)
    private String password;
    
    /**
     * Họ và tên đầy đủ
     */
    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;
    
    /**
     * Email - có thể dùng để đăng nhập hoặc reset password
     */
    @Column(name = "email", length = 255)
    private String email;
    
    /**
     * Số điện thoại
     */
    @Column(name = "phone", length = 20, nullable = false)
    private String phone;
    
    /**
     * Vị trí công việc
     */
    @Column(name = "position", length = 255, nullable = false)
    private String position;
    
    /**
     * Phòng ban
     */
    @Column(name = "department", length = 255, nullable = false)
    private String department;
    
    /**
     * Ngày sinh
     */
    @Column(name = "dob")
    private LocalDate dob;
    
    /**
     * Giới tính: M (Male) hoặc F (Female)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, columnDefinition = "ENUM('M', 'F')")
    private Gender gender;
    
    /**
     * Trạng thái: ACTIVE, RESIGNED, ON_LEAVE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('ACTIVE', 'RESIGNED', 'ON_LEAVE')")
    private StaffStatus status = StaffStatus.ACTIVE;
    
    /**
     * Role của Staff (liên kết với bảng Role)
     * Many-to-One: Nhiều Staff có thể có cùng một Role
     */
    @ManyToOne(fetch = FetchType.EAGER) // EAGER: Load role cùng với staff
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    /**
     * Ngày được tuyển dụng
     */
    @Column(name = "hired_at")
    private LocalDateTime hiredAt;
    
    /**
     * Lần đăng nhập cuối cùng
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    /**
     * Ngày tạo account
     * Tự động set khi tạo mới
     */
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;
    
    /**
     * Token để reset password
     * Không lưu trong database ban đầu, sẽ thêm khi cần
     */
    @Transient // Không map với column trong database
    private String resetPasswordToken;
    
    /**
     * Thời gian hết hạn của reset token
     */
    @Transient
    private LocalDateTime resetPasswordTokenExpiry;
    
    /**
     * Hàm tự động chạy trước khi persist (save) entity
     */
    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
        if (status == null) {
            status = StaffStatus.ACTIVE;
        }
    }
    
    /**
     * Enum cho Gender
     */
    public enum Gender {
        M, // Male
        F  // Female
    }
    
    /**
     * Enum cho Staff Status
     */
    public enum StaffStatus {
        ACTIVE,      // Đang làm việc
        RESIGNED,    // Đã nghỉ việc
        ON_LEAVE     // Đang nghỉ phép
    }
}
