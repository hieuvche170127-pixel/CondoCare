package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Residents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Residents entity
 */
@Repository
public interface ResidentsRepository extends JpaRepository<Residents, String> {
    
    /**
     * Tìm Resident theo username
     */
    Optional<Residents> findByUsername(String username);
    
    /**
     * Tìm Resident theo email
     */
    Optional<Residents> findByEmail(String email);
    
    /**
     * Tìm Resident theo username hoặc email
     */
    Optional<Residents> findByUsernameOrEmail(String username, String email);
    
    /**
     * Kiểm tra username đã tồn tại
     */
    Boolean existsByUsername(String username);
    
    /**
     * Kiểm tra email đã tồn tại
     */
    Boolean existsByEmail(String email);
    
    /**
     * Kiểm tra số CMND/CCCD đã tồn tại
     */
    Boolean existsByIdNumber(String idNumber);
    
    /**
     * Đếm số Resident theo status
     */
    Long countByStatus(Residents.ResidentStatus status);

    Residents findResidentsById (String id);


    @Query("SELECT r FROM Residents r WHERE " +
            "LOWER(r.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.idNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "r.phone LIKE CONCAT('%', :keyword, '%')")
    Page<Residents> searchByKeyword(String keyword, Pageable pageable);
}
