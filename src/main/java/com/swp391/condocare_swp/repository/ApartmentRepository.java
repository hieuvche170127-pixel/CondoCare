package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, String> {
}