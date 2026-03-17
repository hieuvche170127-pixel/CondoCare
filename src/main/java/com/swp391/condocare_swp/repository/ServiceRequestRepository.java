package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.ServiceRequest;
import com.swp391.condocare_swp.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, String> {

    // ── RESIDENT SIDE ─────────────────────────────────────────────

    List<ServiceRequest> findByResidentOrderByCreatedAtDesc(Residents resident);

    List<ServiceRequest> findByResidentAndStatusOrderByCreatedAtDesc(
            Residents resident, ServiceRequest.RequestStatus status);

    long countByResidentAndStatus(Residents resident, ServiceRequest.RequestStatus status);

    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND YEAR(s.createdAt) = :year AND MONTH(s.createdAt) = :month " +
            "ORDER BY s.createdAt DESC")
    List<ServiceRequest> findByResidentAndMonthYear(
            @Param("resident") Residents resident,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND YEAR(s.createdAt) = :year " +
            "AND MONTH(s.createdAt) BETWEEN :monthFrom AND :monthTo " +
            "ORDER BY s.createdAt DESC")
    List<ServiceRequest> findByResidentAndQuarterYear(
            @Param("resident") Residents resident,
            @Param("monthFrom") int monthFrom,
            @Param("monthTo") int monthTo,
            @Param("year") int year);

    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND YEAR(s.createdAt) = :year ORDER BY s.createdAt DESC")
    List<ServiceRequest> findByResidentAndYear(
            @Param("resident") Residents resident,
            @Param("year") int year);

    @Query("SELECT s FROM ServiceRequest s WHERE s.resident = :resident " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
            "OR LOWER(s.description) LIKE LOWER(CONCAT('%',:keyword,'%'))) " +
            "ORDER BY s.createdAt DESC")
    List<ServiceRequest> searchByKeyword(
            @Param("resident") Residents resident,
            @Param("keyword") String keyword);

    // ── STAFF SIDE ────────────────────────────────────────────────

    /** Tất cả yêu cầu, mới nhất trước (có phân trang) */
    Page<ServiceRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Lọc theo status */
    Page<ServiceRequest> findByStatusOrderByCreatedAtDesc(
            ServiceRequest.RequestStatus status, Pageable pageable);

    /** Lọc theo staff được giao */
    Page<ServiceRequest> findByAssignedToOrderByCreatedAtDesc(
            Staff staff, Pageable pageable);

    /** Lọc theo priority */
    Page<ServiceRequest> findByPriorityOrderByCreatedAtDesc(
            ServiceRequest.Priority priority, Pageable pageable);

    /** Tìm kiếm keyword cho staff (toàn hệ thống) */
    @Query("SELECT s FROM ServiceRequest s WHERE " +
            "LOWER(s.title) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
            "OR LOWER(s.description) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
            "OR LOWER(s.resident.fullName) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
            "ORDER BY s.createdAt DESC")
    Page<ServiceRequest> searchAllByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /** Filter kết hợp status + priority */
    @Query("SELECT s FROM ServiceRequest s WHERE " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:priority IS NULL OR s.priority = :priority) AND " +
            "(:assignedToId IS NULL OR s.assignedTo.id = :assignedToId) " +
            "ORDER BY s.createdAt DESC")
    Page<ServiceRequest> filterStaff(
            @Param("status")       ServiceRequest.RequestStatus status,
            @Param("priority")     ServiceRequest.Priority priority,
            @Param("assignedToId") String assignedToId,
            Pageable pageable);

    /** Thống kê theo status */
    long countByStatus(ServiceRequest.RequestStatus status);

    /** Đếm yêu cầu được giao cho 1 staff */
    long countByAssignedTo(Staff staff);

    /** Danh sách yêu cầu chờ xử lý, ưu tiên cao trước */
    @Query("SELECT s FROM ServiceRequest s WHERE s.status = 'PENDING' " +
            "ORDER BY s.priority DESC, s.createdAt ASC")
    List<ServiceRequest> findPendingOrderByPriority();

    /** Yêu cầu đã DONE nhưng resident chưa xác nhận */
    List<ServiceRequest> findByStatusAndResidentConfirmedFalse(
            ServiceRequest.RequestStatus status);
}