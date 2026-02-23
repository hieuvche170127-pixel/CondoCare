package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.ResidentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API cho toàn bộ Resident Dashboard.
 * Base path: /api/resident
 *
 * Mọi endpoint đều dùng JWT để biết "ai đang gọi"
 * → Không cần truyền residentId trong URL.
 */
@RestController
@RequestMapping("/api/resident")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ResidentController {

    @Autowired
    private ResidentDashboardService service;

    /* ─── HOME ─────────────────────────────────────── */

    /** GET /api/resident/home → thống kê nhanh trang chủ */
    @GetMapping("/home")
    public ResponseEntity<?> getHomeSummary() {
        try   { return ResponseEntity.ok(service.getHomeSummary()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /* ─── NOTIFICATIONS ─────────────────────────────── */

    /** GET /api/resident/notifications → danh sách thông báo */
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        try   { return ResponseEntity.ok(service.getNotifications()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** PUT /api/resident/notifications/{id}/read → đánh dấu 1 thông báo đã đọc */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id) {
        try   { service.markNotificationRead(id); return ResponseEntity.ok("OK"); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** PUT /api/resident/notifications/read-all → đánh dấu tất cả đã đọc */
    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> markAllRead() {
        try   { service.markAllNotificationsRead(); return ResponseEntity.ok("OK"); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /* ─── INVOICES ──────────────────────────────────── */

    /** GET /api/resident/invoices → danh sách hóa đơn */
    @GetMapping("/invoices")
    public ResponseEntity<?> getInvoices() {
        try   { return ResponseEntity.ok(service.getInvoices()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /* ─── APARTMENT ─────────────────────────────────── */

    /** GET /api/resident/apartment → thông tin căn hộ */
    @GetMapping("/apartment")
    public ResponseEntity<?> getApartment() {
        try   { return ResponseEntity.ok(service.getApartmentInfo()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /* ─── SERVICE REQUESTS ──────────────────────────── */

    /** GET /api/resident/requests → danh sách yêu cầu hỗ trợ */
    @GetMapping("/requests")
    public ResponseEntity<?> getRequests() {
        try   { return ResponseEntity.ok(service.getServiceRequests()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/resident/requests → tạo yêu cầu mới
     * Body JSON: { "title":"...", "description":"...", "category":"ELECTRIC", "priority":"MEDIUM" }
     */
    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(@RequestBody Map<String, String> body) {
        try {
            String msg = service.createServiceRequest(
                body.get("title"),
                body.get("description"),
                body.getOrDefault("category", "OTHER"),
                body.getOrDefault("priority", "MEDIUM")
            );
            return ResponseEntity.ok(msg);
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}
