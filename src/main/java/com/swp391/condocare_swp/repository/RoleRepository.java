package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Role entity
 * JpaRepository cung cấp các method CRUD cơ bản
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    
    /**
     * Tìm Role theo tên
     * @param name Tên role (VD: ADMIN, MANAGER, STAFF)
     * @return Optional<Role>
     */
    Optional<Role> findByName(String name);
}
