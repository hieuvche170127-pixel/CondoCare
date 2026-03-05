package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.AccessCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessCardRepository extends JpaRepository<AccessCard, String> {

    boolean existsByCardNumber(String cardNumber);

    // Sử dụng LEFT JOIN để tìm kiếm an toàn từ Thẻ -> Cư dân -> Căn hộ -> Tòa nhà
    @Query("SELECT c FROM AccessCard c " +
            "LEFT JOIN c.resident r " +
            "LEFT JOIN r.apartment a " +
            "LEFT JOIN a.building b " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(r.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:buildingId IS NULL OR :buildingId = '' OR b.id = :buildingId)")
    Page<AccessCard> filterAccessCards(@Param("keyword") String keyword,
                                       @Param("status") AccessCard.CardStatus status,
                                       @Param("buildingId") String buildingId,
                                       Pageable pageable);
}