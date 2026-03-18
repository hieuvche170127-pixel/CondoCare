package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, String> {

    /** Tất cả yêu cầu của 1 cư dân, mới nhất trước */
    List<ServiceRequest> findByResidentOrderByCreatedAtDesc(Residents resident);

    /** Lọc theo status */
    List<ServiceRequest> findByResidentAndStatusOrderByCreatedAtDesc(
            Residents resident, ServiceRequest.RequestStatus status);

    /** Đếm yêu cầu theo trạng thái */
    long countByResidentAndStatus(Residents resident, ServiceRequest.RequestStatus status);

    // ── Filter theo thời gian ──────────────────────────────────────

    /** Lọc theo tháng + năm */
    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND YEAR(s.createdAt) = :year AND MONTH(s.createdAt) = :month " +
            "ORDER BY s.createdAt DESC")
    List<ServiceRequest> findByResidentAndMonthYear(
            @Param("resident") Residents resident,
            @Param("month") int month,
            @Param("year") int year);

    /** Lọc theo quý + năm (Q1=1-3, Q2=4-6, Q3=7-9, Q4=10-12) */
    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND YEAR(s.createdAt) = :year " +
            "AND MONTH(s.createdAt) BETWEEN :monthFrom AND :monthTo " +
            "ORDER BY s.createdAt DESC")
    List<ServiceRequest> findByResidentAndQuarterYear(
            @Param("resident") Residents resident,
            @Param("monthFrom") int monthFrom,
            @Param("monthTo") int monthTo,
            @Param("year") int year);

    /** Lọc theo năm */
    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND YEAR(s.createdAt) = :year ORDER BY s.createdAt DESC")
    List<ServiceRequest> findByResidentAndYear(
            @Param("resident") Residents resident,
            @Param("year") int year);

    /** Tìm kiếm theo keyword trong title hoặc description */
    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
            "OR LOWER(s.description) LIKE LOWER(CONCAT('%',:keyword,'%'))) " +
            "ORDER BY s.createdAt DESC")
    List<ServiceRequest> searchByKeyword(
            @Param("resident") Residents resident,
            @Param("keyword") String keyword);
}
