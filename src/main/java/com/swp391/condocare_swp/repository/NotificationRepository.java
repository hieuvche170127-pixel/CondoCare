package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Notification;
import com.swp391.condocare_swp.entity.Residents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    // ── RESIDENT SIDE ─────────────────────────────────────────────────────────

    /**
     * Lấy thông báo của 1 cư dân:
     *   - Thông báo chung (resident IS NULL)
     *   - Thông báo riêng gửi cho cư dân đó
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.resident IS NULL OR n.resident = :resident
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findForResident(@Param("resident") Residents resident);

    /** Đếm thông báo chưa đọc của 1 cư dân */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE (n.resident IS NULL OR n.resident = :resident) AND n.isRead = false
    """)
    long countUnreadForResident(@Param("resident") Residents resident);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") String id);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.isRead = true
        WHERE (n.resident IS NULL OR n.resident = :resident) AND n.isRead = false
    """)
    void markAllAsReadForResident(@Param("resident") Residents resident);

    // ── STAFF SIDE ────────────────────────────────────────────────────────────

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByResidentOrderByCreatedAtDesc(Residents resident);

    List<Notification> findByBuildingIdOrderByCreatedAtDesc(String buildingId);

    List<Notification> findByApartmentIdOrderByCreatedAtDesc(String apartmentId);

    /** Đếm broadcast (resident = NULL) */
    long countByResidentIsNull();

    /** Đếm thông báo cá nhân (resident != NULL) */
    long countByResidentIsNotNull();

    /** Đếm theo trạng thái đọc */
    long countByIsRead(boolean isRead);
}