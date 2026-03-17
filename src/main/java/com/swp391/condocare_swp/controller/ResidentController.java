package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.ResidentDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ResidentController.class);

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

    /**
     * GET /api/resident/invoices → danh sách hóa đơn
     * Query params (tùy chọn):
     *   filterType = all | month | quarter | year
     *   month, quarter, year, status (UNPAID|PAID|OVERDUE|ALL), keyword
     */
    @GetMapping("/invoices")
    public ResponseEntity<?> getInvoices(
            @RequestParam(defaultValue = "all") String filterType,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            return ResponseEntity.ok(
                    service.getInvoices(filterType, month, quarter, year, status, keyword));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* ─── APARTMENT ─────────────────────────────────── */

    /** GET /api/resident/apartment → thông tin căn hộ */
    @GetMapping("/apartment")
    public ResponseEntity<?> getApartment() {
        try   { return ResponseEntity.ok(service.getApartmentInfo()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /* ─── SERVICE REQUESTS ──────────────────────────── */

    /**
     * GET /api/resident/requests → danh sách yêu cầu hỗ trợ
     * Query params (tùy chọn):
     *   filterType = all | month | quarter | year
     *   month      = 1-12
     *   quarter    = 1-4
     *   year       = 2024, 2025, ...
     *   status     = PENDING | IN_PROGRESS | DONE | REJECTED | ALL
     *   keyword    = chuỗi tìm kiếm
     */
    @GetMapping("/requests")
    public ResponseEntity<?> getRequests(
            @RequestParam(defaultValue = "all") String filterType,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            return ResponseEntity.ok(
                    service.getServiceRequests(filterType, month, quarter, year, status, keyword));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/resident/requests → tạo yêu cầu mới
     * Body JSON: { "title":"...", "description":"...", "category":"ELECTRIC", "priority":"MEDIUM" }
     */
    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(@RequestBody Map<String, String> body) {
        logger.info("POST /api/resident/requests — body received: {}", body);
        try {
            String title    = body.get("title");
            String desc     = body.get("description");
            String category = body.getOrDefault("category", "OTHER");
            String priority = body.getOrDefault("priority", "MEDIUM");

            // Log để debug — xác nhận controller nhận đúng data
            logger.info("  title=[{}], desc=[{}], category=[{}], priority=[{}]",
                    title, desc, category, priority);

            String msg = service.createServiceRequest(title, desc, category, priority);
            return ResponseEntity.ok(msg);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Log full stack trace để thấy root cause
            logger.error("Unexpected error creating service request", e);
            return ResponseEntity.internalServerError()
                    .body("Lỗi server: " + e.getMessage() +
                            " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "unknown"));
        }
    }

    /**
     * Đánh dấu hóa đơn đã thanh toán
     * PUT /api/resident/invoices/{id}/pay
     */
    @PutMapping("/invoices/{id}/pay")
    public ResponseEntity<?> markInvoiceAsPaid(@PathVariable String id) {
        try {
            String message = service.markInvoiceAsPaid(id);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error marking invoice as paid", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/resident/requests/{id} → chi tiết yêu cầu (bao gồm ảnh xác nhận)
     */
    @GetMapping("/requests/{id}")
    public ResponseEntity<?> getRequestDetail(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getServiceRequestDetail(id)); }
        catch (Exception e) {
            logger.error("Error getting request detail {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/resident/requests/{id}/confirm
     * Resident xác nhận đã nhận kết quả xử lý.
     */
    @PutMapping("/requests/{id}/confirm")
    public ResponseEntity<?> confirmRequest(@PathVariable String id) {
        try {
            String message = service.confirmServiceRequest(id);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error confirming request {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}