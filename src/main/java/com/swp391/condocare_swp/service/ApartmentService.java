package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApartmentService
{
    private final ApartmentRepository apartmentRepository;

    public Page<Apartment> getApartmentsByBuilding(String buildingId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("number").ascending());

        // Tìm căn hộ theo ID tòa nhà và phân trang
        // Đảm bảo ApartmentRepository đã có hàm: Page<Apartment> findByBuildingId(String bId, Pageable p);
        return apartmentRepository.findByBuildingId(buildingId, pageable);
    }


}
