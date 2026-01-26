package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "staff", schema = "swp391")
public class Staff {
    @Id
    @Column(name = "ID", nullable = false, length = 10)
    private String id;

    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "dob")
    private LocalDate dob;

    @Lob
    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @ColumnDefault("'ACTIVE'")
    @Lob
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "hired_at")
    private Instant hiredAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getHiredAt() {
        return hiredAt;
    }

    public void setHiredAt(Instant hiredAt) {
        this.hiredAt = hiredAt;
    }

    public Instant getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(Instant terminatedAt) {
        this.terminatedAt = terminatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}