package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.StaffCreateRequest;
import com.swp391.condocare_swp.dto.StaffUpdateRequest;
import com.swp391.condocare_swp.service.StaffManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;

@RestController
@RequestMapping("/api/staff-management")
public class StaffManagementController {

    private static final Logger logger = LoggerFactory.getLogger(StaffManagementController.class);
    @Autowired private StaffManagementService service;

    /** GET /api/staff-management/roles — ADMIN + MANAGER (để dropdown khi tạo) */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getRoles() {
        try { return ResponseEntity.ok(service.getAllRoles()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /api/staff-management/stats — ADMIN + MANAGER */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /api/staff-management — ADMIN + MANAGER */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> listStaff(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        try {
            Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sort));
            Page<Map<String, Object>> result = service.listStaff(search, roleId, status, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error listing staff", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /api/staff-management/{id} — ADMIN + MANAGER */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getStaff(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getStaffDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** POST /api/staff-management — chỉ ADMIN */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createStaff(@Valid @RequestBody StaffCreateRequest request) {
        try {
            return ResponseEntity.ok(service.createStaff(request));
        } catch (Exception e) {
            logger.error("Error creating staff", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** PUT /api/staff-management/{id} — chỉ ADMIN */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStaff(@PathVariable String id,
                                         @Valid @RequestBody StaffUpdateRequest request) {
        try {
            return ResponseEntity.ok(service.updateStaff(id, request));
        } catch (Exception e) {
            logger.error("Error updating staff {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** DELETE /api/staff-management/{id} — chỉ ADMIN */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStaff(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.deleteStaff(id));
        } catch (Exception e) {
            logger.error("Error deleting staff {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}