package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Apartment entity
 */
@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, String> {
    
    /**
     * Tìm Apartment theo số căn hộ
     */
    Optional<Apartment> findByNumber(String number);
    
    /**
     * Đếm số Apartment theo status
     */
    Long countByStatus(Apartment.ApartmentStatus status);
    
    /**
     * Đếm số Apartment theo rental status
     */
    Long countByRentalStatus(Apartment.RentalStatus rentalStatus);

    Page<Apartment> findByBuildingId(String buildingId, Pageable pageable);
}
