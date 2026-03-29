package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller quản lý phương tiện phía Staff.
 * Base path: /api/staff/vehicles
 *
 * Phân quyền:
 *   GET stats / list / pending → ADMIN, MANAGER, RECEPTIONIST
 *   POST approve / reject      → ADMIN, MANAGER, RECEPTIONIST
 *   POST revoke                → ADMIN, MANAGER (không bao gồm RECEPTIONIST)
 *
 * Đã khớp với SecurityConfig: /api/staff/vehicles/** → ADMIN, MANAGER, RECEPTIONIST
 */
@RestController
@RequestMapping("/api/staff/vehicles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffVehicleController {

    private static final Logger logger = LoggerFactory.getLogger(StaffVehicleController.class);

    @Autowired
    private VehicleService service;

    // ── THỐNG KÊ ──────────────────────────────────────────────────────────────

    /**
     * GET /api/staff/vehicles/stats
     * Trả về: { pending, approved, rejected, total }
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> getStats() {
        try {
            return ResponseEntity.ok(service.getStats());
        } catch (Exception e) {
            logger.error("Error loading vehicle stats", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── DANH SÁCH ─────────────────────────────────────────────────────────────

    /**
     * GET /api/staff/vehicles/pending
     * Lấy danh sách xe đang chờ duyệt (pendingStatus = PENDING).
     * Dùng cho tab "Chờ duyệt" trên UI.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> getPending() {
        try {
            return ResponseEntity.ok(service.getPendingVehicles());
        } catch (Exception e) {
            logger.error("Error loading pending vehicles", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/staff/vehicles
     * Lấy toàn bộ xe với các bộ lọc tuỳ chọn.
     *
     * Query params:
     *   type          = MOTORBIKE | CAR | BICYCLE | ELECTRIC_BIKE | OTHER
     *   pendingStatus = PENDING | APPROVED | REJECTED
     *   status        = ACTIVE | INACTIVE | REVOKED | LOST
     *   apartmentId   = APT001 (lọc theo căn hộ)
     *
     * Dùng cho tab "Tất cả xe" có bộ lọc trên UI.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> getAllVehicles(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String pendingStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String apartmentId) {
        try {
            return ResponseEntity.ok(service.getAllVehicles(type, pendingStatus, status, apartmentId));
        } catch (Exception e) {
            logger.error("Error listing vehicles", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── HÀNH ĐỘNG ─────────────────────────────────────────────────────────────

    /**
     * POST /api/staff/vehicles/{id}/approve
     * Duyệt đăng ký xe, tính ngày hết hạn, gửi thông báo cho cư dân.
     * Body JSON: { "note": "Ghi chú tuỳ chọn" }
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> approve(@PathVariable String id,
                                     @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = (body != null) ? body.get("note") : null;
            return ResponseEntity.ok(service.approveVehicle(id, note));
        } catch (Exception e) {
            logger.error("Error approving vehicle {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/staff/vehicles/{id}/reject
     * Từ chối đăng ký xe, gửi thông báo lý do cho cư dân.
     * Body JSON: { "reason": "Lý do từ chối (bắt buộc)" }
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            if (reason == null || reason.isBlank())
                return ResponseEntity.badRequest().body("Lý do từ chối không được để trống.");
            return ResponseEntity.ok(service.rejectVehicle(id, reason));
        } catch (Exception e) {
            logger.error("Error rejecting vehicle {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/staff/vehicles/{id}/revoke
     * Thu hồi đăng ký xe đã duyệt — chỉ ADMIN + MANAGER.
     * Body JSON: { "reason": "Lý do thu hồi (bắt buộc)" }
     */
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> revoke(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            if (reason == null || reason.isBlank())
                return ResponseEntity.badRequest().body("Lý do thu hồi không được để trống.");
            return ResponseEntity.ok(service.revokeVehicle(id, reason));
        } catch (Exception e) {
            logger.error("Error revoking vehicle {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}