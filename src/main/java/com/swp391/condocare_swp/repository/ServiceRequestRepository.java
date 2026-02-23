package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, String> {

    /** Tất cả yêu cầu của 1 cư dân, mới nhất trước */
    List<ServiceRequest> findByResidentOrderByCreatedAtDesc(Residents resident);

    /** Đếm yêu cầu theo trạng thái */
    long countByResidentAndStatus(Residents resident, ServiceRequest.RequestStatus status);
}
