package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.ResidentDashboardService;
import com.swp391.condocare_swp.service.VehicleService;
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
 * Phân chia trách nhiệm:
 *   - ResidentDashboardService : home, notifications, invoices, apartment, service requests
 *   - VehicleService           : đăng ký xe + xem danh sách xe (tránh trùng lặp logic)
 */
@RestController
@RequestMapping("/api/resident")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ResidentController {

    private static final Logger logger = LoggerFactory.getLogger(ResidentController.class);

    @Autowired
    private ResidentDashboardService service;

    @Autowired
    private VehicleService vehicleService;

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

    /**
     * GET /api/resident/apartment → thông tin căn hộ.
     * Response không kèm danh sách xe — dùng GET /api/resident/vehicles riêng.
     */
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

            logger.info("  title=[{}], desc=[{}], category=[{}], priority=[{}]",
                    title, desc, category, priority);

            String msg = service.createServiceRequest(title, desc, category, priority);
            return ResponseEntity.ok(msg);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error creating service request", e);
            return ResponseEntity.internalServerError()
                    .body("Lỗi server: " + e.getMessage() +
                            " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "unknown"));
        }
    }

    /** PUT /api/resident/invoices/{id}/pay — ĐÃ XÓA
     *
     * [FIX #2] Endpoint này cho phép resident tự đánh dấu hóa đơn là PAID mà không
     * thực sự thanh toán, tạo ra lỗ hổng nghiệp vụ nghiêm trọng.
     *
     * Luồng thanh toán hợp lệ duy nhất là qua MoMo:
     *   1. Resident gọi POST /api/momo/create-payment  → nhận payUrl / deeplink
     *   2. Resident hoàn thành thanh toán trên app MoMo
     *   3. MoMo gọi POST /api/momo/ipn (IPN callback) → MomoService cập nhật Invoice → PAID
     *
     * Nếu cần Staff đánh dấu PAID thủ công (ví dụ: thanh toán tiền mặt),
     * dùng: PATCH /api/invoice-management/{id}/status  (chỉ ADMIN/MANAGER)
     */

    /** GET /api/resident/requests/{id} → chi tiết yêu cầu (bao gồm ảnh xác nhận) */
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

    /**
     * PUT /api/resident/requests/{id}/cancel
     * Resident hủy yêu cầu hỗ trợ khi còn ở trạng thái PENDING.
     * [FIX #11] Trước đây resident không có cách hủy yêu cầu đã tạo nhầm/không cần nữa.
     * Chỉ được hủy khi status = PENDING (chưa có nhân viên xử lý).
     */
    @PutMapping("/requests/{id}/cancel")
    public ResponseEntity<?> cancelRequest(@PathVariable String id) {
        try {
            String message = service.cancelServiceRequest(id);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error cancelling request {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* ─── VEHICLES ───────────────────────────────────── */

    /**
     * GET /api/resident/vehicles → danh sách xe của cư dân đang đăng nhập.
     * Delegate hoàn toàn cho VehicleService (nguồn dữ liệu duy nhất cho vehicle).
     */
    @GetMapping("/vehicles")
    public ResponseEntity<?> getMyVehicles() {
        try {
            return ResponseEntity.ok(vehicleService.getMyVehicles());
        } catch (Exception e) {
            logger.error("Error loading resident vehicles", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/resident/vehicles → đăng ký gửi xe mới.
     * Delegate hoàn toàn cho VehicleService — không còn xử lý trong ResidentDashboardService.
     * Body JSON: {
     *   "type": "MOTORBIKE|CAR|BICYCLE|ELECTRIC_BIKE|OTHER",
     *   "licensePlate": "29B1-12345",   // optional
     *   "brand": "Honda",               // optional
     *   "model": "Wave",                // optional
     *   "color": "Đen",                 // optional
     *   "durationType": "MONTHLY|QUARTERLY|YEARLY"
     * }
     */
    @PostMapping("/vehicles")
    public ResponseEntity<?> registerVehicle(@RequestBody Map<String, String> body) {
        logger.info("POST /api/resident/vehicles — body: {}", body);
        try {
            String msg = vehicleService.registerVehicle(body);
            return ResponseEntity.ok(msg);
        } catch (IllegalArgumentException | RuntimeException e) {
            logger.warn("Error registering vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error registering vehicle", e);
            return ResponseEntity.internalServerError()
                    .body("Lỗi server: " + e.getMessage());
        }
    }
}