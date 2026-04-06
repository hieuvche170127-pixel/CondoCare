package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.StaffServiceRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * StaffServiceRequestController
 *
 * THAY ĐỔI: Tách isTechnician(auth) thành private helper dùng chung
 * thay vì inline stream().anyMatch() lặp lại 3 lần.
 * Logic nghiệp vụ (assertAssignedTo) vẫn ở Service.
 */
@RestController
@RequestMapping("/api/staff/requests")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffServiceRequestController {

    private static final Logger logger = LoggerFactory.getLogger(StaffServiceRequestController.class);

    @Autowired private StaffServiceRequestService service;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','TECHNICIAN','ACCOUNTANT')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/assignable-staff")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> getAssignableStaff() {
        try { return ResponseEntity.ok(service.getAssignableStaff()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','TECHNICIAN','ACCOUNTANT')")
    public ResponseEntity<?> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignedToId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // [THAY ĐỔI] helper method thay vì inline
            String effectiveAssignedToId = isTechnician(auth)
                    ? service.getStaffIdByUsername(auth.getName())
                    : assignedToId;
            return ResponseEntity.ok(
                    service.listRequests(status, priority, effectiveAssignedToId, keyword, page, size));
        } catch (Exception e) {
            logger.error("Error listing service requests", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','TECHNICIAN','ACCOUNTANT')")
    public ResponseEntity<?> getDetail(@PathVariable String id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (isTechnician(auth)) {
                service.assertAssignedTo(id, service.getStaffIdByUsername(auth.getName()));
            }
            return ResponseEntity.ok(service.getDetail(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> assign(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            String assigneeId = body.get("assigneeId");
            if (assigneeId == null || assigneeId.isBlank())
                return ResponseEntity.badRequest().body("assigneeId không được để trống");
            return ResponseEntity.ok(service.assignAndStart(id, assigneeId, body.get("note")));
        } catch (Exception e) {
            logger.error("Error assigning request {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> reject(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(service.reject(id, body.get("rejectReason")));
        } catch (Exception e) {
            logger.error("Error rejecting request {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/done")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public ResponseEntity<?> markDone(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (isTechnician(auth)) {
                service.assertAssignedTo(id, service.getStaffIdByUsername(auth.getName()));
            }
            return ResponseEntity.ok(service.markDone(id, body.get("completionImage"), body.get("note")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error marking request {} as done", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public ResponseEntity<?> updateNote(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (isTechnician(auth)) {
                service.assertAssignedTo(id, service.getStaffIdByUsername(auth.getName()));
            }
            return ResponseEntity.ok(service.updateNote(id, body.get("note")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── PRIVATE HELPER ───────────────────────────────────────────────────────

    /**
     * [NEW] Kiểm tra Authentication hiện tại có role TECHNICIAN không.
     * Trước: 3 method đều inline stream().anyMatch() giống nhau.
     * Sau: dùng method này dùng chung — dễ đổi nếu cần.
     */
    private boolean isTechnician(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));
    }
}