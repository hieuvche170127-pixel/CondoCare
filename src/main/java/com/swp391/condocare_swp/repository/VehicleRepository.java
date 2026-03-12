package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    // Kiểm tra trùng biển số (ngoại trừ chính xe đang sửa)
    @Query("SELECT COUNT(v) > 0 FROM Vehicle v WHERE v.licensePlate = :licensePlate AND v.id <> :id")
    boolean existsByLicensePlateAndNotId(@Param("licensePlate") String licensePlate, @Param("id") String id);

    boolean existsByLicensePlate(String licensePlate);

    // Lọc theo từ khóa (Biển số hoặc Tên chủ xe) và Trạng thái
    @Query("SELECT v FROM Vehicle v LEFT JOIN v.resident r " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR v.status = :status)")
    Page<Vehicle> filterVehicles(@Param("keyword") String keyword,
                                 @Param("status") Vehicle.VehicleStatus status,
                                 Pageable pageable);
}