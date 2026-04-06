package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.ApartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller quản lý Tòa nhà (Building), Căn hộ (Apartment)
 * và Mẫu phí dịch vụ (FeeTemplate).
 *
 * Base path: /api  (giữ nguyên để không break URL hiện có)
 *
 * Nhóm endpoint:
 *   Building     : /api/buildings/**
 *   Apartment    : /api/apartments/**
 *   FeeTemplate  : /api/fee-templates/**   ← endpoint DUY NHẤT cho fee-templates
 *                                             (InvoiceManagementController đã bỏ endpoint trùng lặp)
 *
 * THAY ĐỔI so với bản cũ:
 *   - Không có thay đổi code — giữ nguyên hoàn toàn.
 *   - Chỉ cập nhật comment để ghi rõ /api/fee-templates là endpoint duy nhất
 *     sau khi đã xóa endpoint trùng trong InvoiceManagementController.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApartmentController {

    private static final Logger logger = LoggerFactory.getLogger(ApartmentController.class);
    @Autowired private ApartmentService service;

    // ══════════════════════════════════════════════════════════════════════════
    // STATS
    // ══════════════════════════════════════════════════════════════════════════

    /** GET /api/apartments/stats */
    @GetMapping("/apartments/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BUILDING
    // ══════════════════════════════════════════════════════════════════════════

    /** GET /api/buildings */
    @GetMapping("/buildings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getAllBuildings() {
        try { return ResponseEntity.ok(service.getAllBuildings()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /api/buildings/{id} */
    @GetMapping("/buildings/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getBuildingDetail(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getBuildingDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/buildings
     * Body: { name, address, totalFloors, totalApartments, managerId }
     */
    @PostMapping("/buildings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createBuilding(@RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.createBuilding(body)); }
        catch (Exception e) {
            logger.error("Error creating building", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/buildings/{id}
     * Body: { name?, address?, totalFloors?, totalApartments?, managerId? }
     */
    @PutMapping("/buildings/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateBuilding(@PathVariable String id,
                                            @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.updateBuilding(id, body)); }
        catch (Exception e) {
            logger.error("Error updating building {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // APARTMENT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/apartments
     * Params: buildingId (optional), status (optional: EMPTY|OCCUPIED|MAINTENANCE)
     */
    @GetMapping("/apartments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getAllApartments(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String status) {
        try { return ResponseEntity.ok(service.getAllApartments(buildingId, status)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * GET /api/apartments/{id}
     * Trả về chi tiết căn hộ kèm danh sách phí áp dụng và ước tính chi phí tháng.
     */
    @GetMapping("/apartments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getApartmentDetail(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getApartmentDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/apartments
     * Body: { buildingId, number, floor, area, description? }
     * Tự động kiểm tra trùng số căn hộ trong cùng tòa nhà.
     */
    @PostMapping("/apartments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createApartment(@RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.createApartment(body)); }
        catch (Exception e) {
            logger.error("Error creating apartment", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/apartments/{id}
     * Body: { floor?, area?, status?, rentalStatus?, description? }
     */
    @PutMapping("/apartments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateApartment(@PathVariable String id,
                                             @RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.updateApartment(id, body)); }
        catch (Exception e) {
            logger.error("Error updating apartment {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEE TEMPLATE  — Endpoint DUY NHẤT cho fee-templates trong toàn project
    // (InvoiceManagementController đã xóa endpoint /api/invoice-management/fee-templates
    //  trùng lặp. Frontend dùng các URL dưới đây cho mọi thao tác với fee-templates.)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/fee-templates?buildingId=BLD001&status=ACTIVE
     * Lấy danh sách mẫu phí của tòa nhà.
     * status: ACTIVE | INACTIVE (tuỳ chọn)
     *
     * ACCOUNTANT được phép xem (dùng để preview hóa đơn).
     */
    @GetMapping("/fee-templates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getFeeTemplates(
            @RequestParam String buildingId,
            @RequestParam(required = false) String status) {
        try { return ResponseEntity.ok(service.getFeeTemplates(buildingId, status)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * GET /api/fee-templates/hanoi-decree
     * Trả về mức phí gợi ý theo Quyết định 33/2025/QĐ-UBND UBND TP Hà Nội.
     */
    @GetMapping("/fee-templates/hanoi-decree")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getHanoiDecreeSuggestions() {
        try { return ResponseEntity.ok(service.getHanoiDecreeSuggestions()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/fee-templates
     * Body: {
     *   buildingId, name, type (SERVICE|PARKING),
     *   unit (PER_M2|PER_APT|FIXED), amount,
     *   effectiveFrom (yyyy-MM-dd), effectiveTo? (yyyy-MM-dd),
     *   hasElevator? (true|false)
     * }
     */
    @PostMapping("/fee-templates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createFeeTemplate(@RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.createFeeTemplate(body)); }
        catch (Exception e) {
            logger.error("Error creating fee template", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/fee-templates/{id}
     * Body: { name?, type?, unit?, amount?, effectiveFrom?, effectiveTo?, status? }
     */
    @PutMapping("/fee-templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateFeeTemplate(@PathVariable String id,
                                               @RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.updateFeeTemplate(id, body)); }
        catch (Exception e) {
            logger.error("Error updating fee template {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * DELETE /api/fee-templates/{id}
     * Soft-delete: chuyển status → INACTIVE.
     */
    @DeleteMapping("/fee-templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> deactivateFeeTemplate(@PathVariable String id) {
        try { return ResponseEntity.ok(service.deactivateFeeTemplate(id)); }
        catch (Exception e) {
            logger.error("Error deactivating fee template {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}