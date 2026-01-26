package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "meter_reading", schema = "swp391")
public class MeterReading {
    @Id
    @Column(name = "ID", nullable = false, length = 10)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Lob
    @Column(name = "meter_type", nullable = false)
    private String meterType;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "previous_index", nullable = false, precision = 19)
    private BigDecimal previousIndex;

    @Column(name = "current_index", nullable = false, precision = 19)
    private BigDecimal currentIndex;

    @Column(name = "total_amount", nullable = false)
    private Float totalAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Apartment getApartment() {
        return apartment;
    }

    public void setApartment(Apartment apartment) {
        this.apartment = apartment;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getPreviousIndex() {
        return previousIndex;
    }

    public void setPreviousIndex(BigDecimal previousIndex) {
        this.previousIndex = previousIndex;
    }

    public BigDecimal getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(BigDecimal currentIndex) {
        this.currentIndex = currentIndex;
    }

    public Float getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Float totalAmount) {
        this.totalAmount = totalAmount;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

}