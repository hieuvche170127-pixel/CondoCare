package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, String> {

    List<Building> findByManagerId(String managerId);

    boolean existsByName(String name);
}