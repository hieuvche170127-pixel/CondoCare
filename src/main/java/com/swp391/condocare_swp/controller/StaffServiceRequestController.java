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
 * REST Controller quản lý yêu cầu hỗ trợ phía Staff.
 *
 * Phân quyền:
 *   ADMIN / MANAGER    — toàn quyền: xem tất cả, phân công, từ chối, đánh dấu xong
 *   RECEPTIONIST       — xem tất cả, PHÂN CÔNG kỹ thuật viên (không reject/done)
 *   TECHNICIAN         — chỉ xem yêu cầu được phân công cho mình, đánh dấu xong
 *   ACCOUNTANT         — chỉ xem danh sách (read-only, không hành động)
 */
@RestController
@RequestMapping("/api/staff/requests")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffServiceRequestController {

    private static final Logger logger = LoggerFactory.getLogger(StaffServiceRequestController.class);

    @Autowired
    private StaffServiceRequestService service;

    // ─── THỐNG KÊ ─────────────────────────────────────────────────────────────

    /** GET /stats — tất cả staff */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','TECHNICIAN','ACCOUNTANT')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // ─── DANH SÁCH KỸ THUẬT VIÊN (để dropdown phân công) ─────────────────────

    /**
     * GET /assignable-staff
     * ADMIN, MANAGER, RECEPTIONIST dùng để chọn người phân công.
     */
    @GetMapping("/assignable-staff")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public ResponseEntity<?> getAssignableStaff() {
        try { return ResponseEntity.ok(service.getAssignableStaff()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // ─── DANH SÁCH YÊU CẦU ────────────────────────────────────────────────────

    /**
     * GET /
     * - TECHNICIAN: tự động lọc chỉ các yêu cầu được phân công cho mình
     *   (service đọc username từ SecurityContext và filter theo assigned_to)
     * - Các role khác: xem tất cả
     */
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
            boolean isTechnician = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));

            // TECHNICIAN chỉ thấy yêu cầu của mình — ghi đè bộ lọc assignedToId
            String effectiveAssignedToId = assignedToId;
            if (isTechnician) {
                // Service sẽ tự lookup staffId từ username
                effectiveAssignedToId = service.getStaffIdByUsername(auth.getName());
            }

            return ResponseEntity.ok(
                    service.listRequests(status, priority, effectiveAssignedToId, keyword, page, size));
        } catch (Exception e) {
            logger.error("Error listing service requests", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── CHI TIẾT ─────────────────────────────────────────────────────────────

    /**
     * GET /{id}
     * TECHNICIAN chỉ được xem yêu cầu được phân công cho mình.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','TECHNICIAN','ACCOUNTANT')")
    public ResponseEntity<?> getDetail(@PathVariable String id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isTechnician = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));

            if (isTechnician) {
                // Kiểm tra yêu cầu có được phân công cho technician này không
                String staffId = service.getStaffIdByUsername(auth.getName());
                service.assertAssignedTo(id, staffId);
            }

            return ResponseEntity.ok(service.getDetail(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── PHÂN CÔNG ────────────────────────────────────────────────────────────

    /**
     * POST /{id}/assign
     * ADMIN, MANAGER, RECEPTIONIST được phân công kỹ thuật viên.
     * TECHNICIAN không được tự phân công.
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
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

    // ─── TỪ CHỐI ─────────────────────────────────────────────────────────────

    /**
     * POST /{id}/reject — chỉ ADMIN + MANAGER
     * RECEPTIONIST không được từ chối (chỉ phân công).
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

    // ─── HOÀN THÀNH ────────────────────────────────────────────────────────────

    /**
     * POST /{id}/done
     * ADMIN, MANAGER, TECHNICIAN (người được phân công) mới được đánh dấu xong.
     */
    @PostMapping("/{id}/done")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public ResponseEntity<?> markDone(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isTechnician = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));

            // TECHNICIAN chỉ được đánh dấu xong yêu cầu của mình
            if (isTechnician) {
                String staffId = service.getStaffIdByUsername(auth.getName());
                service.assertAssignedTo(id, staffId);
            }

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

    // ─── GHI CHÚ ─────────────────────────────────────────────────────────────

    /**
     * PATCH /{id}/note
     * ADMIN, MANAGER, TECHNICIAN có thể ghi chú.
     */
    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public ResponseEntity<?> updateNote(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isTechnician = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));

            if (isTechnician) {
                String staffId = service.getStaffIdByUsername(auth.getName());
                service.assertAssignedTo(id, staffId);
            }

            return ResponseEntity.ok(service.updateNote(id, body.get("note")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}