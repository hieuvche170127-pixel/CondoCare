package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho Building entity
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, String> {
    
    /**
     * Đếm tổng số Building
     */
    // count() method được kế thừa từ JpaRepository
}
