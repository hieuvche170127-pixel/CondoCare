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
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;

    // Lấy danh sách TẤT CẢ căn hộ (Có tìm kiếm theo số phòng và phân trang)
    public Page<Apartment> getApartmentsPaginated(String keyword, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("building.name").ascending().and(Sort.by("number").ascending()));

        if (keyword != null && !keyword.trim().isEmpty()) {
            return apartmentRepository.findByNumberContainingIgnoreCase(keyword.trim(), pageable);
        }
        return apartmentRepository.findAll(pageable);
    }

    // Lấy 1 căn hộ theo ID
    public Apartment getApartmentById(String id) {
        return apartmentRepository.findById(id).orElse(null);
    }

    // Lưu căn hộ
    public void saveApartment(Apartment apartment) {
        apartmentRepository.save(apartment);
    }

    // Xóa căn hộ (Xóa vật lý)
    public void deleteApartment(String id) {
        apartmentRepository.deleteById(id);
    }

    // Thêm hàm này vào trong ApartmentService.java

    // Kiểm tra xem căn hộ đã tồn tại chưa
    public boolean checkDuplicateApartment(String buildingId, Integer floor, String number) {
        return apartmentRepository.existsByBuildingIdAndFloorAndNumber(buildingId, floor, number);
    }

    public Page<Apartment> filterApartments(String keyword, String buildingId,
                                            Apartment.ApartmentStatus status,
                                            Apartment.RentalStatus rentalStatus,
                                            int pageNo, int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize,
                Sort.by("building.name").ascending().and(Sort.by("number").ascending()));

        // Gọi hàm Filter thông minh từ Repository
        return apartmentRepository.filterApartments(keyword, buildingId, status, rentalStatus, pageable);
    }
}
