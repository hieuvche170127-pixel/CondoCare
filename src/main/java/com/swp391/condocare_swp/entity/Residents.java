package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Residents")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Residents {

    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;

    @Column(name = "username", length = 255, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "ENUM('OWNER','TENANT','GUEST')")
    private ResidentType type = ResidentType.TENANT;

    @Column(name = "dob")
    @Past(message = "Ngày sinh không hợp lệ")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, columnDefinition = "ENUM('M','F')")
    private Gender gender;

    @Column(name = "id_number", length = 12)
    @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải bao gồm 12 chữ số")
    private String idNumber;

    @Column(name = "phone", length = 20, nullable = false)
    @Pattern(regexp = "^(0)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    /**
     * PENDING  = Vừa đăng ký, chờ Manager duyệt
     * ACTIVE   = Đã được xác minh, dùng được đầy đủ tính năng
     * INACTIVE = Đã bị vô hiệu hóa / rời tòa nhà
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('PENDING','ACTIVE','INACTIVE')")
    private ResidentStatus status = ResidentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    /** Staff đã duyệt tài khoản này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private Staff verifiedBy;

    /** Thời điểm được duyệt */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = ResidentStatus.PENDING;
        if (type   == null) type   = ResidentType.TENANT;
    }

    // ── Enums ────────────────────────────────────────────────

    public enum ResidentType { OWNER, TENANT, GUEST }

    public enum Gender { M, F }

    public enum ResidentStatus {
        PENDING,   // Chờ manager duyệt
        ACTIVE,    // Đang hoạt động
        INACTIVE   // Không hoạt động
    }
}