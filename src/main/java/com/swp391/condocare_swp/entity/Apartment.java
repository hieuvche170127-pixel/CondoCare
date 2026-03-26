package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Apartment",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_apartment_number_building",
                columnNames = {"number", "building_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Apartment {

    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;

    @Column(name = "number", length = 10, nullable = false)
    private String number;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Column(name = "area", nullable = false, precision = 8, scale = 2)
    private BigDecimal area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('EMPTY','OCCUPIED','MAINTENANCE')")
    private ApartmentStatus status = ApartmentStatus.EMPTY;

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_status", nullable = false,
            columnDefinition = "ENUM('AVAILABLE','RENTED','OWNER_OCCUPIED')")
    private RentalStatus rentalStatus = RentalStatus.AVAILABLE;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "total_residents", nullable = false)
    private Integer totalResident = 0;

    @Column(name = "total_vehicles", nullable = false)
    private Integer totalVehicle = 0;

    @PrePersist
    protected void onCreate() {
        if (status        == null) status        = ApartmentStatus.EMPTY;
        if (rentalStatus  == null) rentalStatus  = RentalStatus.AVAILABLE;
        if (totalResident == null) totalResident = 0;
        if (totalVehicle  == null) totalVehicle  = 0;
    }

    public enum ApartmentStatus { EMPTY, OCCUPIED, MAINTENANCE }
    public enum RentalStatus    { AVAILABLE, RENTED, OWNER_OCCUPIED }
}