package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Vehicle;
import com.swp391.condocare_swp.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public Page<Vehicle> filterVehicles(String keyword, Vehicle.VehicleStatus status, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("registeredAt").descending());
        return vehicleRepository.filterVehicles(keyword, status, pageable);
    }

    public Vehicle getVehicleById(String id) {
        return vehicleRepository.findById(id).orElse(null);
    }

    public boolean checkDuplicateLicensePlate(String licensePlate, String id) {
        if (id == null || id.isEmpty()) {
            return vehicleRepository.existsByLicensePlate(licensePlate); // Lúc thêm mới
        }
        return vehicleRepository.existsByLicensePlateAndNotId(licensePlate, id); // Lúc update
    }

    public void saveVehicle(Vehicle vehicle) {
        vehicleRepository.save(vehicle);
    }

    // Hủy đăng ký phương tiện
    public void revokeVehicle(String id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
        if (vehicle != null) {
            vehicle.setStatus(Vehicle.VehicleStatus.INACTIVE);
            vehicle.setRevokedAt(LocalDateTime.now()); // Lưu lại thời điểm hủy
            vehicleRepository.save(vehicle);
        }
    }
}