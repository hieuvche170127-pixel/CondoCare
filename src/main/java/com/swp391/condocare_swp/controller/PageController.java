package com.swp391.condocare_swp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller trả về các trang HTML
 */
@Controller
public class PageController {

    /** GET / → Trang chủ (HomePage) */
    @GetMapping("/")
    public String homePage() {
        return "index";
    }

    /** GET /login → Trang đăng nhập */
    @GetMapping("/login")
    public String authLoginPage() {
        return "login";
    }

    /** GET /register → Trang đăng ký */
    @GetMapping("/register")
    public String authRegisterPage() {
        return "register";
    }

    /** GET /forgot-password → Trang quên mật khẩu */
    @GetMapping("/forgot-password")
    public String authForgotPasswordPage() {
        return "forgot-password";
    }

    /** GET /reset-password → Trang reset mật khẩu */
    @GetMapping("/reset-password")
    public String authResetPasswordPage() {
        return "reset-password";
    }

    /** GET /dashboard → Trang tổng quan (Staff) */
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }

    /** GET /profile → Trang thông tin cá nhân (dùng chung staff + resident) */
    @GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }

    // ─── RESIDENT DASHBOARD PAGES ────────────────────────────────────────

    /** GET /resident → Trang chủ cư dân */
    @GetMapping("/resident")
    public String residentHome() {
        return "resident/home";
    }

    /** GET /resident/notifications → Thông báo của cư dân */
    @GetMapping("/resident/notifications")
    public String residentNotifications() {
        return "resident/notifications";
    }

    /** GET /resident/invoices → Hóa đơn của cư dân */
    @GetMapping("/resident/invoices")
    public String residentInvoices() {
        return "resident/invoices";
    }

    /** GET /resident/apartment → Thông tin căn hộ */
    @GetMapping("/resident/apartment")
    public String residentApartment() {
        return "resident/apartment";
    }

    /** GET /resident/requests → Yêu cầu hỗ trợ của cư dân */
    @GetMapping("/resident/requests")
    public String residentRequests() {
        return "resident/requests";
    }

    /**
     * GET /resident/vehicles → Quản lý phương tiện của cư dân
     * [FIX] Thêm route còn thiếu — backend đã có POST /api/resident/vehicles
     *       nhưng chưa có trang UI để cư dân truy cập.
     */
    @GetMapping("/resident/vehicles")
    public String residentVehicles() {
        return "resident/vehicles";
    }

    // ─── STAFF DASHBOARD PAGES ───────────────────────────────────────────

    /**
     * GET /dashboard/staff → Quản lý nhân viên
     */
    @GetMapping("/dashboard/staff")
    public String staffManagement() {
        return "staff/list";
    }

    /**
     * GET /dashboard/resident → Quản lý cư dân (phía staff)
     * [FIX] Đổi template từ "resident/list" → "staff/residents"
     *       để đúng vị trí thư mục theo convention staff pages.
     */
    @GetMapping("/dashboard/resident")
    public String residentManagement() {
        return "staff/residents";
    }

    /** GET /dashboard/invoices → Quản lý hóa đơn */
    @GetMapping("/dashboard/invoices")
    public String invoiceManagement() {
        return "staff/invoices";
    }

    /** GET /dashboard/apartments → Quản lý căn hộ & phí dịch vụ */
    @GetMapping("/dashboard/apartments")
    public String apartmentManagement() {
        return "staff/apartments";
    }

    /** GET /dashboard/service-requests → Quản lý yêu cầu hỗ trợ */
    @GetMapping("/dashboard/service-requests")
    public String staffServiceRequests() {
        return "staff/service-requests";
    }

    /**
     * GET /dashboard/vehicles → Quản lý phương tiện
     *       để tránh lỗi case-sensitive trên Linux server.
     */
    @GetMapping("/dashboard/vehicles")
    public String vehicleManagement() {
        return "staff/vehicles";
    }

    /**
     * GET /dashboard/notifications → Quản lý thông báo
     *       để tránh lỗi case-sensitive trên Linux server.
     */
    @GetMapping("/dashboard/notifications")
    public String notificationManagement() {
        return "staff/notifications";
    }
}