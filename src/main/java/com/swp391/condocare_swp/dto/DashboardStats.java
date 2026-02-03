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
    
    /**
     * Tổng số căn hộ
     */
    private Long totalApartments;
    
    /**
     * Số căn hộ trống
     */
    private Long emptyApartments;
    
    /**
     * Số căn hộ đang có người ở
     */
    private Long occupiedApartments;
    
    /**
     * Số căn hộ đang bảo trì
     */
    private Long maintenanceApartments;
    
    /**
     * Tổng số cư dân
     */
    private Long totalResidents;
    
    /**
     * Số cư dân hoạt động
     */
    private Long activeResidents;
    
    /**
     * Tổng số nhân viên
     */
    private Long totalStaff;
    
    /**
     * Số nhân viên đang làm việc
     */
    private Long activeStaff;
    
    /**
     * Tổng số tòa nhà
     */
    private Long totalBuildings;
}
