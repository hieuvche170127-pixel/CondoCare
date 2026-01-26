package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Table(name = "residents", schema = "swp391")
public class Resident {
    @Id
    @Column(name = "ID", nullable = false, length = 10)
    private String id;

    @Lob
    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "dob")
    private LocalDate dob;

    @Lob
    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "id_number", nullable = false, length = 12)
    private String idNumber;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @ColumnDefault("'ACTIVE'")
    @Lob
    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "temp_residence")
    private String tempResidence;

    @Column(name = "temp_absence")
    private String tempAbsence;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
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

    public Apartment getApartment() {
        return apartment;
    }

    public void setApartment(Apartment apartment) {
        this.apartment = apartment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTempResidence() {
        return tempResidence;
    }

    public void setTempResidence(String tempResidence) {
        this.tempResidence = tempResidence;
    }

    public String getTempAbsence() {
        return tempAbsence;
    }

    public void setTempAbsence(String tempAbsence) {
        this.tempAbsence = tempAbsence;
    }

}