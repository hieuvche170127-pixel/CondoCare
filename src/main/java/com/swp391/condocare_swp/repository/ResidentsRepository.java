package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Residents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResidentsRepository
        extends JpaRepository<Residents, String>, JpaSpecificationExecutor<Residents> {

    Optional<Residents> findByUsername(String username);

    Optional<Residents> findByEmail(String email);

    Optional<Residents> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    long countByStatus(Residents.ResidentStatus status);

    long countByType(Residents.ResidentType type);

    Page<Residents> findByStatus(Residents.ResidentStatus status, Pageable pageable);

    Page<Residents> findByApartmentId(String apartmentId, Pageable pageable);

    /** Dùng cho InvoiceScheduler — lấy tất cả cư dân ACTIVE trong căn hộ để gửi notification */
    java.util.List<Residents> findByApartmentIdAndStatus(String apartmentId, Residents.ResidentStatus status);
}