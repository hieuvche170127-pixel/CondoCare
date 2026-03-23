package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final BuildingRepository buildingRepository;
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateRandomId() {
        StringBuilder sb = new StringBuilder("BLD");
        for (int i = 0; i <7; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public List<Apartment> getAllApartments() {
        return apartmentRepository.findAll();
    }

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
    public void saveApartment(Apartment apartment){

        Building building = buildingRepository.findById(apartment.getBuilding().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà"));

        //2. Kiểm tra số tầng hợp lệ
        if (apartment.getFloor() > building.getTotalFloors()) {
            throw new RuntimeException("Tầng" + apartment.getFloor() +
                    "vượt quá số tầng tối đa của tòa nhà(" + building.getTotalFloors() + ")");
        }

        if(apartment.getFloor() <= 0) {
            throw new RuntimeException("Số tầng phải lớn hơn 0");
        }
        //3. Check trùng số phòng cùng 1 tòa nhà, cùng 1 tầng
        if (apartment.getId() == null || apartment.getId().isEmpty()) { //chỉ check khi thêm mới
            boolean isDuplicate = apartmentRepository.existsByBuildingIdAndFloorAndNumber(
                    building.getId(), apartment.getFloor(), apartment.getNumber());
            if (isDuplicate){
                throw new RuntimeException("Căn hộ số " + apartment.getNumber() +
                        " tại tầng " + apartment.getFloor() + " đã tồn tại trong tòa nhà này!");
            }
        }

        //4. Validate diện tích
        if (apartment.getArea() == null || apartment.getArea() <= 0) {
            throw new RuntimeException("Diện tích căn hộ phải lớn hơn 0");
        }

        //5. Validate số tiền (compareTo)
        if (apartment.getRentPrice() != null && apartment.getRentPrice().compareTo(BigDecimal.ZERO) < 0){
            throw new RuntimeException("Giá bán không được là số âm");
        }

        if (apartment.getSalePrice() != null && apartment.getSalePrice().compareTo(BigDecimal.ZERO) < 0){
            throw new RuntimeException("Giá bán không được là số âm");
        }


        if (apartment.getId() == null || apartment.getId().trim().isEmpty()) {
            String newId;
            boolean isUnique = false;

            while (!isUnique) {
                newId = generateRandomId();
                if (!apartmentRepository.existsById(newId)){
                    apartment.setId(newId);
                    isUnique = true;
                }
            }
        }


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
    public List<Apartment> getApartmentsByBuilding(String buildingId) {
        return apartmentRepository.findByBuildingId(buildingId);
    }


}
