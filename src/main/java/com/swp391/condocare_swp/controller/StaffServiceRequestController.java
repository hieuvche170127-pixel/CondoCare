package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.StaffServiceRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/requests")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffServiceRequestController {

    private static final Logger logger = LoggerFactory.getLogger(StaffServiceRequestController.class);

    @Autowired
    private StaffServiceRequestService service;

    /** GET /stats — tất cả staff */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /assignable-staff — ADMIN + MANAGER (để chọn người phân công) */
    @GetMapping("/assignable-staff")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getAssignableStaff() {
        try { return ResponseEntity.ok(service.getAssignableStaff()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET / — tất cả staff */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignedToId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(
                    service.listRequests(status, priority, assignedToId, keyword, page, size));
        } catch (Exception e) {
            logger.error("Error listing service requests", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /{id} — tất cả staff */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getDetail(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /{id}/assign — chỉ ADMIN + MANAGER
     * STAFF không được tự phân công (tránh tự assign cho mình)
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> assign(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String assigneeId = body.get("assigneeId");
            String note       = body.get("note");
            if (assigneeId == null || assigneeId.isBlank())
                return ResponseEntity.badRequest().body("assigneeId không được để trống");
            return ResponseEntity.ok(service.assignAndStart(id, assigneeId, note));
        } catch (Exception e) {
            logger.error("Error assigning request {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /{id}/reject — chỉ ADMIN + MANAGER
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> reject(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("rejectReason");
            return ResponseEntity.ok(service.reject(id, reason));
        } catch (Exception e) {
            logger.error("Error rejecting request {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /{id}/done — tất cả staff (người được giao thực hiện)
     */
    @PostMapping("/{id}/done")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','TECHNICIAN')")
    public ResponseEntity<?> markDone(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String image = body.get("completionImage");
            String note  = body.get("note");
            return ResponseEntity.ok(service.markDone(id, image, note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error marking request {} as done", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PATCH /{id}/note — tất cả staff
     */
    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','TECHNICIAN')")
    public ResponseEntity<?> updateNote(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(service.updateNote(id, body.get("note")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}