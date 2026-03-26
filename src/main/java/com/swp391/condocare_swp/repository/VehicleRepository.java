package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository
        extends JpaRepository<Vehicle, String>, JpaSpecificationExecutor<Vehicle> {

    List<Vehicle> findByResidentId(String residentId);

    List<Vehicle> findByApartmentId(String apartmentId);

    List<Vehicle> findByPendingStatus(Vehicle.PendingStatus pendingStatus);

    long countByApartmentIdAndStatus(String apartmentId, Vehicle.VehicleStatus status);

    boolean existsByLicensePlate(String licensePlate);
}