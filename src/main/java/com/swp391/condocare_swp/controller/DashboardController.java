package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.DashboardStats;
import com.swp391.condocare_swp.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cho Dashboard API
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * API lấy thống kê Dashboard
     * GET /api/dashboard/stats
     *
     * [FIX] Role 'STAFF' không tồn tại trong DB — đã thay bằng đúng 5 role thực tế.
     *       Role trong DB: ADMIN, MANAGER, ACCOUNTANT, TECHNICIAN, RECEPTIONIST
     *       SecurityConfig tầng URL đã đúng (dùng ALL_STAFF array), chỉ annotation
     *       method-level này bị sai → ACCOUNTANT, TECHNICIAN, RECEPTIONIST bị chặn oan.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'TECHNICIAN', 'RECEPTIONIST')")
    public ResponseEntity<?> getDashboardStats() {
        try {
            DashboardStats stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}