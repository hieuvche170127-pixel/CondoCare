package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.FeeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeTemplateRepository extends JpaRepository<FeeTemplate, String> {

    List<FeeTemplate> findByBuildingIdAndStatus(String buildingId, FeeTemplate.FeeStatus status);

    List<FeeTemplate> findByBuildingId(String buildingId);
}