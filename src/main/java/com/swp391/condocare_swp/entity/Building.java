package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Building")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Building {

    @Id
    @Column(name = "ID", length = 10, nullable = false)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "total_floors", nullable = false)
    private Integer totalFloors;

    @Column(name = "total_apartments", nullable = false)
    private Integer totalApartments;

    /** Trưởng ban quản lý tòa nhà */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Staff manager;
}