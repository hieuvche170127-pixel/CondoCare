package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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

    Optional<Apartment> findByNumber(String number);

    // Nếu số căn có thể trùng giữa các tòa → tìm chính xác hơn
    Optional<Apartment> findByNumberIgnoreCase(String number);

    /**
     * Tìm căn hộ theo số căn (case-insensitive).
     * Lưu ý: number chỉ unique trong phạm vi 1 tòa (uq_apartment_number_building),
     *         nên nếu toàn hệ thống có >1 căn cùng số thì trả về first match.
     */
    Optional<Apartment> findFirstByNumberIgnoreCase(String number);

    /**
     * Tìm theo số căn + buildingId — chính xác hơn khi nhiều tòa.
     */
    Optional<Apartment> findByNumberIgnoreCaseAndBuilding_Id(String number, String buildingId);

    /**
     * Danh sách tất cả căn hộ, sắp xếp theo tòa → số căn.
     * Dùng để populate dropdown chọn căn hộ trong form tạo hóa đơn.
     */
    List<Apartment> findAllByOrderByBuilding_NameAscNumberAsc();

    /**
     * Danh sách căn hộ đang OCCUPIED (có người ở) — filter hữu ích khi tạo hóa đơn.
     */
    @Query("SELECT a FROM Apartment a WHERE a.status = 'OCCUPIED' " +
            "ORDER BY a.building.name ASC, a.number ASC")
    List<Apartment> findAllOccupiedOrderByBuildingAndNumber();
}