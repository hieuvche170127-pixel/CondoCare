package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, String> {

    // Quản lý: Thấy tất cả trừ DRAFT của người khác
    @Query("SELECT c FROM Contract c WHERE " +
            "(c.status != 'DRAFT' OR c.staff.id = :managerId) AND " +
            "(:apartmentId IS NULL OR :apartmentId = '' OR c.apartment.id = :apartmentId)")
    Page<Contract> findContractsForManager(@Param("managerId") String managerId,
                                           @Param("apartmentId") String apartmentId,
                                           Pageable pageable);

    // Cư dân: Chỉ thấy hợp đồng của mình (Không bao giờ thấy DRAFT)
    @Query("SELECT c FROM Contract c WHERE c.resident.id = :residentId AND c.status != 'DRAFT'")
    Page<Contract> findContractsForResident(@Param("residentId") String residentId, Pageable pageable);

    // Dùng cho tự động thanh lý hợp đồng quá hạn
    List<Contract> findByStatusAndEndDateBefore(Contract.ContractStatus status, LocalDate date);
}