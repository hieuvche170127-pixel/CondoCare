package com.swp391.condocare_swp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller trả về các trang HTML
 */
@Controller
public class PageController {

    /**
     * Trang chủ (HomePage)
     * GET /
     */
    @GetMapping("/")
    public String homePage() {
        return "index";
    }

    /**
     * Trang đăng nhập
     * GET /login
     */
    @GetMapping("/auth/login")
    public String authLoginPage() {
        return "login";
    }

    /**
     * Trang đăng ký
     * GET /register
     */
    @GetMapping("/auth/register")
    public String authRegisterPage() {
        return "register";
    }

    /**
     * Trang quên mật khẩu
     * GET /forgot-password
     */
    @GetMapping("/auth/forgot-password")
    public String authForgotPasswordPage() {
        return "forgot-password";
    }

    /**
     * Trang reset mật khẩu
     * GET /reset-password
     */
    @GetMapping("/auth/reset-password")
    public String authResetPasswordPage() {
        return "reset-password";
    }

    /**
     * Trang Dashboard (Staff)
     * GET /dashboard
     */
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }

    /**
     * Trang Profile (Thông tin cá nhân)
     * GET /profile
     */
    @GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }

    // ─── RESIDENT DASHBOARD PAGES ───────────────────────────

    /** GET /resident → Trang chủ cư dân */
    @GetMapping("/resident")
    public String residentHome()          { return "resident/home"; }

    /** GET /resident/notifications → Thông báo */
    @GetMapping("/resident/notifications")
    public String residentNotifications() { return "resident/notifications"; }

    /** GET /resident/invoices → Hóa đơn */
    @GetMapping("/resident/invoices")
    public String residentInvoices()      { return "resident/invoices"; }

    /** GET /resident/apartment → Thông tin căn hộ */
    @GetMapping("/resident/apartment")
    public String residentApartment()     { return "resident/apartment"; }

    /** GET /resident/requests → Yêu cầu hỗ trợ */
    @GetMapping("/resident/requests")
    public String residentRequests()      { return "resident/requests"; }

    // ─── STAFF ACCOUNT MANAGEMENT PAGES ────────────────────

    /** GET /dashboard/staff → Quản lý nhân viên */
    @GetMapping("/dashboard/staff")
    public String staffManagement()       { return "staff/list"; }

    /** GET /dashboard/resident → Quản lý cư dân (phía staff) */
    @GetMapping("/dashboard/resident")
    public String residentManagement()    { return "resident/list"; }

    /** GET /dashboard/invoices → Quản lý hóa đơn */
    @GetMapping("/dashboard/invoices")
    public String invoiceManagement()     { return "staff/invoices"; }

    /** GET /dashboard/service-requests → Quan ly yeu cau ho tro */
    @GetMapping("/dashboard/service-requests")
    public String staffServiceRequests()  { return "staff/service-requests"; }

}