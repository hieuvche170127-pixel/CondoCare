package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller quản lý thông báo phía Staff.
 * Base: /api/staff/notifications
 *
 * Chức năng:
 *   - Xem tất cả thông báo đã gửi (có filter)
 *   - Gửi broadcast toàn tòa
 *   - Gửi theo căn hộ
 *   - Gửi cá nhân cho 1 cư dân
 *   - Xóa thông báo
 *   - Thống kê
 */
@RestController
@RequestMapping("/api/staff/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(StaffNotificationController.class);
    @Autowired private NotificationService service;

    // ── THỐNG KÊ ──────────────────────────────────────────────────────────────

    /** GET /api/staff/notifications/stats */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // ── DANH SÁCH ─────────────────────────────────────────────────────────────

    /**
     * GET /api/staff/notifications
     * Params (tùy chọn):
     *   type        = INFO | WARNING | URGENT | MAINTENANCE | PAYMENT | ALL
     *   buildingId  = lọc theo tòa nhà
     *   residentId  = lọc theo cư dân cụ thể
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getAllNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String residentId) {
        try {
            return ResponseEntity.ok(service.getAllNotifications(type, buildingId, residentId));
        } catch (Exception e) {
            logger.error("Error listing notifications", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GỬI THÔNG BÁO ─────────────────────────────────────────────────────────

    /**
     * POST /api/staff/notifications/broadcast
     * Gửi cho TOÀN BỘ cư dân trong tòa nhà.
     * Body: { "title": "...", "content": "...", "type": "INFO|URGENT|...", "buildingId": "BLD001" }
     */
    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> sendBroadcast(@RequestBody Map<String, String> body) {
        try {
            logger.info("Staff sending broadcast to building: {}", body.get("buildingId"));
            return ResponseEntity.ok(service.sendBroadcast(body));
        } catch (Exception e) {
            logger.error("Error sending broadcast", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/staff/notifications/apartment
     * Gửi cho tất cả cư dân trong 1 căn hộ.
     * Body: { "title": "...", "content": "...", "type": "INFO", "apartmentId": "APT001" }
     */
    @PostMapping("/apartment")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> sendToApartment(@RequestBody Map<String, String> body) {
        try {
            logger.info("Staff sending notification to apartment: {}", body.get("apartmentId"));
            return ResponseEntity.ok(service.sendToApartment(body));
        } catch (Exception e) {
            logger.error("Error sending apartment notification", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/staff/notifications/resident
     * Gửi cá nhân cho 1 cư dân.
     * Body: { "title": "...", "content": "...", "type": "INFO", "residentId": "RES001" }
     */
    @PostMapping("/resident")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> sendToResident(@RequestBody Map<String, String> body) {
        try {
            logger.info("Staff sending personal notification to resident: {}", body.get("residentId"));
            return ResponseEntity.ok(service.sendToResident(body));
        } catch (Exception e) {
            logger.error("Error sending personal notification", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── XÓA ───────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/staff/notifications/{id}
     * Chỉ ADMIN / MANAGER được xóa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> deleteNotification(@PathVariable String id) {
        try { return ResponseEntity.ok(service.deleteNotification(id)); }
        catch (Exception e) {
            logger.error("Error deleting notification {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}