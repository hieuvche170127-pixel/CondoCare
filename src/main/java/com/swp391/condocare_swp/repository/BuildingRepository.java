package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho Building entity
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, String> {
    Building findBuildingById(String id);

    Page<Building> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    List<Building> findByNameContainingIgnoreCase(String keyword);
    /**
     * Đếm tổng số Building
     */
    // count() method được kế thừa từ JpaRepository
}
