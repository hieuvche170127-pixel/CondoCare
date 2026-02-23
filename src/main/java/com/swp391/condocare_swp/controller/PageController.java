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
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    /**
     * Trang đăng ký
     * GET /register
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    
    /**
     * Trang quên mật khẩu
     * GET /forgot-password
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }
    
    /**
     * Trang reset mật khẩu
     * GET /reset-password
     */
    @GetMapping("/reset-password")
    public String resetPasswordPage() {
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
}
