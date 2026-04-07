package com.swp391.condocare_swp.security;

import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityUtils — Helper dùng chung để lấy thông tin user đang đăng nhập.
 *
 * Lý do tách class này:
 *   Trước đây mỗi Service tự copy-paste đoạn code:
 *     SecurityContextHolder.getContext().getAuthentication().getName()
 *     + staffRepo.findByUsername(...) / residentsRepo.findByUsername(...)
 *   → Trùng lặp ở 10+ chỗ: VehicleService, NotificationService, ApartmentService,
 *     InvoiceManagementService, ResidentManagementService, StaffManagementService,
 *     ResidentDashboardService, StaffServiceRequestService, ProfileService, AuthService.
 *
 * Cách dùng trong Service:
 *   @Autowired private SecurityUtils securityUtils;
 *   ...
 *   Staff staff = securityUtils.getCurrentStaff();
 *   Residents r = securityUtils.getCurrentResident();
 *   String username = securityUtils.getCurrentUsername();
 */
@Component
public class SecurityUtils {

    @Autowired
    private StaffRepository staffRepo;

    @Autowired
    private ResidentsRepository residentsRepo;

    /**
     * Trả về tên đăng nhập (username) của user đang đăng nhập hiện tại.
     *
     * @throws RuntimeException nếu không có Authentication hợp lệ trong SecurityContext.
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Không có phiên đăng nhập hợp lệ.");
        }
        return auth.getName();
    }

    /**
     * Trả về Staff đang đăng nhập.
     * Dùng trong các Service phía Staff (StaffVehicleController, NotificationService, ...).
     *
     * @throws RuntimeException nếu không tìm thấy Staff với username hiện tại.
     */
    public Staff getCurrentStaff() {
        String username = getCurrentUsername();
        return staffRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy thông tin nhân viên đang đăng nhập: " + username));
    }

    /**
     * Trả về Residents đang đăng nhập.
     * Dùng trong các Service phía Resident (ResidentDashboardService, VehicleService, ...).
     *
     * @throws RuntimeException nếu không tìm thấy Residents với username hiện tại.
     */
    public Residents getCurrentResident() {
        String username = getCurrentUsername();
        return residentsRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy thông tin cư dân đang đăng nhập: " + username));
    }

    /**
     * Kiểm tra xem user hiện tại có vai trò (role) cụ thể không.
     * Dùng trong Service khi cần phân nhánh logic theo role.
     *
     * Ví dụ: securityUtils.hasRole("TECHNICIAN")
     *
     * @param role Tên role KHÔNG có prefix "ROLE_" (e.g. "ADMIN", "TECHNICIAN")
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}