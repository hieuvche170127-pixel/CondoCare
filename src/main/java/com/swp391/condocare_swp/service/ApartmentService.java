package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.ApartmentDto;
import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApartmentService {

    @Autowired
    private ApartmentRepository apartmentRepository;

    public List<ApartmentDto> getAllApartmentsDto() {
        return apartmentRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Apartment createApartment(Apartment apartment) {
        return apartmentRepository.save(apartment);
    }

    public Apartment updateApartment(String id, Apartment updated) {
        Apartment existing = apartmentRepository.findById(id).orElseThrow();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MGR"));

        if (isManager) {
            // Full update
            existing.setNumber(updated.getNumber());
            existing.setFloor(updated.getFloor());
            existing.setArea(updated.getArea());
            // ... other fields
        }
        // Luôn update status/rentalStatus
        existing.setStatus(updated.getStatus());
        existing.setRentalStatus(updated.getRentalStatus());

        return apartmentRepository.save(existing);
    }

    public void deleteApartment(String id) {
        apartmentRepository.deleteById(id);
    }

    private ApartmentDto toDto(Apartment apt) {
        ApartmentDto dto = new ApartmentDto();
        dto.setId(apt.getId());
        dto.setNumber(apt.getNumber());
        dto.setFloor(apt.getFloor());
        dto.setArea(apt.getArea());
        dto.setStatus(apt.getStatus().name());
        dto.setRentalStatus(apt.getRentalStatus().name());
        dto.setTotalResident(apt.getTotalResident());
        dto.setBuildingId(apt.getBuilding().getId());
        return dto;
    }
}