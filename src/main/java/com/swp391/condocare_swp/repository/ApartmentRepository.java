package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<Apartment> findByNumberContainingIgnoreCase(String number, Pageable pageable);

    // HÀM MỚI: Tự động check trùng lặp theo Tòa nhà, Tầng, và Số phòng
    boolean existsByBuildingIdAndFloorAndNumber(String buildingId, Integer floor, String number);

    @Query("SELECT a FROM Apartment a WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(a.number) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:buildingId IS NULL OR :buildingId = '' OR a.building.id = :buildingId) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:rentalStatus IS NULL OR a.rentalStatus = :rentalStatus)")
    Page<Apartment> filterApartments(@Param("keyword") String keyword,
                                     @Param("buildingId") String buildingId,
                                     @Param("status") Apartment.ApartmentStatus status,
                                     @Param("rentalStatus") Apartment.RentalStatus rentalStatus,
                                     Pageable pageable);
}

