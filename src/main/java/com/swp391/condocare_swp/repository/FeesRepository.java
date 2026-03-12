package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Fees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeesRepository extends JpaRepository<Fees, String> {

    /** Tất cả fee của 1 căn hộ */
    List<Fees> findByApartment(Apartment apartment);

    /** Fee theo loại của 1 căn hộ */
    List<Fees> findByApartmentAndType(Apartment apartment, Fees.FeeType type);

    /** Tất cả fee đang có hiệu lực (effectiveTo = null hoặc >= hôm nay) */
    List<Fees> findByApartmentAndEffectiveToIsNull(Apartment apartment);
}