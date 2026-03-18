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

    /**
     * Lấy tất cả thông báo dành cho 1 cư dân cụ thể:
     *  - Thông báo chung (resident IS NULL)  ← dành cho mọi người
     *  - Thông báo riêng gửi cho cư dân đó (resident = residentObj)
     * Sắp xếp mới nhất lên đầu
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.resident IS NULL OR n.resident = :resident
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findForResident(@Param("resident") Residents resident);

    /**
     * Đếm thông báo chưa đọc của 1 cư dân
     */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE (n.resident IS NULL OR n.resident = :resident) AND n.isRead = false
    """)
    long countUnreadForResident(@Param("resident") Residents resident);

    /**
     * Đánh dấu đã đọc 1 thông báo
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") String id);

    /**
     * Đánh dấu tất cả đã đọc cho 1 cư dân
     */
    @Modifying
    @Query("""
        UPDATE Notification n SET n.isRead = true
        WHERE (n.resident IS NULL OR n.resident = :resident) AND n.isRead = false
    """)
    void markAllAsReadForResident(@Param("resident") Residents resident);
}
