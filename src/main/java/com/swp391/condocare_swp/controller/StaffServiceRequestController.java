package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.StaffServiceRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API cho Staff quản lý yêu cầu hỗ trợ.
 * Base path: /api/staff/requests
 *
 * Luồng:
 *   GET    /                   → danh sách (filter, pagination)
 *   GET    /stats              → thống kê nhanh
 *   GET    /assignable-staff   → danh sách staff có thể phân công
 *   GET    /{id}               → chi tiết (bao gồm ảnh)
 *   POST   /{id}/assign        → phân công + chuyển IN_PROGRESS
 *   POST   /{id}/reject        → từ chối
 *   POST   /{id}/done          → hoàn thành + upload ảnh
 *   PATCH  /{id}/note          → cập nhật ghi chú
 */
@RestController
@RequestMapping("/api/staff/requests")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffServiceRequestController {

    private static final Logger logger = LoggerFactory.getLogger(StaffServiceRequestController.class);

    @Autowired
    private StaffServiceRequestService service;

    /** GET /api/staff/requests/stats */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /api/staff/requests/assignable-staff */
    @GetMapping("/assignable-staff")
    public ResponseEntity<?> getAssignableStaff() {
        try { return ResponseEntity.ok(service.getAssignableStaff()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * GET /api/staff/requests
     * Params: status, priority, assignedToId (or "me"), keyword, page, size
     */
    @GetMapping
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

    /** GET /api/staff/requests/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/staff/requests/{id}/assign
     * Body: { "assigneeId": "S003", "note": "..." }
     */
    @PostMapping("/{id}/assign")
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
     * POST /api/staff/requests/{id}/reject
     * Body: { "rejectReason": "..." }
     */
    @PostMapping("/{id}/reject")
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
     * POST /api/staff/requests/{id}/done
     * Body: {
     *   "completionImage": "data:image/jpeg;base64,...",
     *   "note": "Đã thay bóng đèn mới loại LED 9W"
     * }
     */
    @PostMapping("/{id}/done")
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
     * PATCH /api/staff/requests/{id}/note
     * Body: { "note": "..." }
     */
    @PatchMapping("/{id}/note")
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