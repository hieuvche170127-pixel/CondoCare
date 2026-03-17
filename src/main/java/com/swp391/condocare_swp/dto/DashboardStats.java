package com.swp391.condocare_swp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho thống kê Dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    // ── Căn hộ ──────────────────────────────────────────────
    private Long totalApartments;
    private Long emptyApartments;
    private Long occupiedApartments;
    private Long maintenanceApartments;

    // ── Cư dân ──────────────────────────────────────────────
    private Long totalResidents;
    private Long activeResidents;

    // ── Nhân viên ───────────────────────────────────────────
    private Long totalStaff;
    private Long activeStaff;

    // ── Tòa nhà ─────────────────────────────────────────────
    private Long totalBuildings;

    // ── Hóa đơn ─────────────────────────────────────────────
    private Long paidInvoices;
    private Long unpaidInvoices;
    private Long overdueInvoices;

    // ── Yêu cầu hỗ trợ ──────────────────────────────────────
    private Long pendingRequests;
    private Long inProgressRequests;
    private Long doneRequests;
}