package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.ResidentCreateRequest;
import com.swp391.condocare_swp.dto.ResidentUpdateRequest;
import com.swp391.condocare_swp.service.ResidentManagementService;
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

@RestController
@RequestMapping("/api/resident-management")
public class ResidentManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ResidentManagementController.class);
    @Autowired private ResidentManagementService service;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> listResidents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String apartmentId,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction dir = "desc".equalsIgnoreCase(direction)
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sort));
            return ResponseEntity.ok(service.listResidents(search, type, status, apartmentId, pageable));
        } catch (Exception e) {
            logger.error("Error listing residents", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ACCOUNTANT','TECHNICIAN','RECEPTIONIST')")
    public ResponseEntity<?> getResident(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getResidentDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createResident(@Valid @RequestBody ResidentCreateRequest request) {
        try { return ResponseEntity.ok(service.createResident(request)); }
        catch (Exception e) {
            logger.error("Error creating resident", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateResident(@PathVariable String id,
                                            @Valid @RequestBody ResidentUpdateRequest request) {
        try { return ResponseEntity.ok(service.updateResident(id, request)); }
        catch (Exception e) {
            logger.error("Error updating resident {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> deactivateResident(@PathVariable String id) {
        try { return ResponseEntity.ok(service.deactivateResident(id)); }
        catch (Exception e) {
            logger.error("Error deactivating resident {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── LUỒNG DUYỆT PENDING ───────────────────────────────────────────────────

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> approveResident(@PathVariable String id,
                                             @RequestBody(required = false) Map<String, String> body) {
        try {
            String apartmentId = body != null ? body.get("apartmentId") : null;
            String type        = body != null ? body.get("type")        : null;
            String note        = body != null ? body.get("note")        : null;
            return ResponseEntity.ok(service.approveResident(id, apartmentId, type, note));
        } catch (Exception e) {
            logger.error("Error approving resident {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> rejectResident(@PathVariable String id,
                                            @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", "Thông tin không hợp lệ.");
            return ResponseEntity.ok(service.rejectResident(id, reason));
        } catch (Exception e) {
            logger.error("Error rejecting resident {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── RESET MẬT KHẨU ───────────────────────────────────────────────────────

    /**
     * POST /api/resident-management/{id}/reset-password
     * Tự sinh mật khẩu random → lưu DB → gửi email cho cư dân.
     * Yêu cầu cư dân phải có email mới gửi được.
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> resetPassword(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.resetPassword(id));
        } catch (Exception e) {
            logger.error("Error resetting password for resident {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}