package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.InvoiceCreateRequest;
import com.swp391.condocare_swp.service.InvoiceManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller quản lý hóa đơn (phía Staff) — Mô hình B.
 *
 * Phân quyền:
 *   GET  (xem danh sách, thống kê, chi tiết, preview) → ADMIN, MANAGER, ACCOUNTANT
 *   POST (tạo hóa đơn)                                → ADMIN, MANAGER
 *   PATCH (cập nhật trạng thái)                       → ADMIN, MANAGER
 *   DELETE (xóa hóa đơn)                              → ADMIN, MANAGER
 *
 * ACCOUNTANT chỉ có quyền XEM — không tạo/sửa/xóa.
 *
 * FIX BUG 1:
 *   - Thêm GET /apartment-list để frontend populate dropdown chọn căn hộ.
 *   - Preview + Create vẫn gọi service.resolveApartment() — chấp nhận cả ID lẫn số căn.
 */
@RestController
@RequestMapping("/api/invoice-management")
public class InvoiceManagementController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceManagementController.class);
    @Autowired private InvoiceManagementService service;

    // ─── ĐỌC (ACCOUNTANT + ADMIN + MANAGER) ──────────────────────────────────

    /** GET /api/invoice-management/stats */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * GET /api/invoice-management
     * Params: page, size, search, status, apartmentId, month, year, sort, direction
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<?> listInvoices(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String apartmentId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "year") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sort));
            return ResponseEntity.ok(
                    service.listInvoices(search, status, apartmentId, month, year, pageable));
        } catch (Exception e) {
            logger.error("Error listing invoices", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /api/invoice-management/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getInvoiceDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * GET /api/invoice-management/preview?apartmentId=APT001&month=6&year=2025
     *
     * FIX BUG 1: apartmentId giờ chấp nhận cả apartment ID ("APT001") lẫn
     *            apartment number ("A101") — service.previewInvoice sẽ tự resolve.
     *
     * ACCOUNTANT xem được preview — chỉ để kiểm tra, không tạo được hóa đơn.
     */
    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<?> previewInvoice(
            @RequestParam String apartmentId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        try { return ResponseEntity.ok(service.previewInvoice(apartmentId, month, year)); }
        catch (Exception e) {
            logger.error("Error previewing invoice for apartment='{}' {}/{}",
                    apartmentId, month, year, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/invoice-management/apartment-list
     *
     * FIX BUG 1 (NEW): Trả về danh sách tất cả căn hộ dạng gọn để frontend
     *                   populate dropdown chọn căn hộ trong form tạo hóa đơn.
     *                   Trả về: [{id, number, buildingName, area, status}, ...]
     */
    @GetMapping("/apartment-list")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<?> getApartmentList() {
        try { return ResponseEntity.ok(service.getApartmentList()); }
        catch (Exception e) {
            logger.error("Error fetching apartment list", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/invoice-management/fee-templates?buildingId=BLD001
     */
    @GetMapping("/fee-templates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT')")
    public ResponseEntity<?> getActiveFeeTemplates(@RequestParam String buildingId) {
        try { return ResponseEntity.ok(service.getActiveFeesForBuilding(buildingId)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // ─── GHI (chỉ ADMIN + MANAGER) ────────────────────────────────────────────

    /**
     * POST /api/invoice-management
     *
     * FIX BUG 1: apartmentId trong request body giờ cũng chấp nhận cả ID lẫn
     *            number nhờ service.resolveApartment() — nhưng nếu dùng dropdown
     *            đúng thì luôn gửi ID chính xác.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createInvoice(@Valid @RequestBody InvoiceCreateRequest request) {
        try {
            logger.info("POST /api/invoice-management — apt: '{}', {}/{}",
                    request.getApartmentId(), request.getMonth(), request.getYear());
            return ResponseEntity.ok(service.createInvoice(request));
        } catch (Exception e) {
            logger.error("Error creating invoice for apt='{}' {}/{}",
                    request.getApartmentId(), request.getMonth(), request.getYear(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PATCH /api/invoice-management/{id}/status
     * Đánh dấu trạng thái — ACCOUNTANT KHÔNG được thay đổi.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null) return ResponseEntity.badRequest().body("Thiếu trường 'status'.");
            return ResponseEntity.ok(service.updateStatus(id, newStatus));
        } catch (Exception e) {
            logger.error("Error updating invoice status {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * DELETE /api/invoice-management/{id}
     * Chỉ ADMIN + MANAGER — không bao gồm ACCOUNTANT.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> deleteInvoice(@PathVariable String id) {
        try { return ResponseEntity.ok(service.deleteInvoice(id)); }
        catch (Exception e) {
            logger.error("Error deleting invoice {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}