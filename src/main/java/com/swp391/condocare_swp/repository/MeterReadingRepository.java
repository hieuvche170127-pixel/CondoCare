package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, String> {

    /** Tìm chỉ số điện/nước theo căn hộ + loại + tháng + năm */
    Optional<MeterReading> findByApartmentAndMeterTypeAndMonthAndYear(
            Apartment apartment, MeterReading.MeterType meterType,
            Integer month, Integer year);

    /** Danh sách chỉ số của 1 căn hộ theo loại, mới nhất trước */
    List<MeterReading> findByApartmentAndMeterTypeOrderByYearDescMonthDesc(
            Apartment apartment, MeterReading.MeterType meterType);

    /** Tất cả chỉ số của 1 căn hộ trong tháng/năm */
    List<MeterReading> findByApartmentAndMonthAndYear(
            Apartment apartment, Integer month, Integer year);
}