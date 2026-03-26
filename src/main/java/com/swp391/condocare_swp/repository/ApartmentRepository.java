package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApartmentRepository
        extends JpaRepository<Apartment, String>, JpaSpecificationExecutor<Apartment> {

    List<Apartment> findByBuildingId(String buildingId);

    List<Apartment> findByBuildingIdAndStatus(String buildingId, Apartment.ApartmentStatus status);

    Optional<Apartment> findByNumberAndBuildingId(String number, String buildingId);

    long countByStatus(Apartment.ApartmentStatus status);

    long countByBuildingId(String buildingId);
}