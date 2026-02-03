package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Staff entity
 */
@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    
    /**
     * Tìm Staff theo username
     * @param username Username
     * @return Optional<Staff>
     */
    Optional<Staff> findByUsername(String username);
    
    /**
     * Tìm Staff theo email
     * @param email Email
     * @return Optional<Staff>
     */
    Optional<Staff> findByEmail(String email);
    
    /**
     * Tìm Staff theo username hoặc email
     * @param username Username
     * @param email Email
     * @return Optional<Staff>
     */
    Optional<Staff> findByUsernameOrEmail(String username, String email);
    
    /**
     * Kiểm tra username đã tồn tại chưa
     * @param username Username
     * @return true nếu đã tồn tại
     */
    Boolean existsByUsername(String username);
    
    /**
     * Kiểm tra email đã tồn tại chưa
     * @param email Email
     * @return true nếu đã tồn tại
     */
    Boolean existsByEmail(String email);
    
    /**
     * Đếm số Staff theo status
     * @param status Staff Status
     * @return Số lượng
     */
    Long countByStatus(Staff.StaffStatus status);
}
