package com.swp391.condocare_swp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service để gửi email
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi email reset password
     * @param toEmail Email người nhận
     * @param resetToken Reset token
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            // Tạo reset link
            String resetLink = "http://localhost:8080/reset-password?token=" + resetToken;

            // Tạo email message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Reset Password - Apartment Management System");
            message.setText("Xin chào,\n\n" +
                    "Bạn đã yêu cầu reset password cho tài khoản của mình.\n\n" +
                    "Vui lòng click vào link dưới đây để reset password:\n" +
                    resetLink + "\n\n" +
                    "Link này sẽ hết hạn sau 1 giờ.\n\n" +
                    "Nếu bạn không yêu cầu reset password, vui lòng bỏ qua email này.\n\n" +
                    "Trân trọng,\n" +
                    "Apartment Management Team");

            // Gửi email
            mailSender.send(message);

            logger.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Error sending password reset email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }

    /**
     * Gửi email thông báo reset password thành công
     */
    public void sendPasswordResetSuccessEmail(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Successful");
            message.setText("Xin chào,\n\n" +
                    "Password của bạn đã được reset thành công.\n\n" +
                    "Nếu bạn không thực hiện hành động này, vui lòng liên hệ với quản trị viên ngay lập tức.\n\n" +
                    "Trân trọng,\n" +
                    "Apartment Management Team");

            mailSender.send(message);

            logger.info("Password reset success email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Error sending password reset success email to: {}", toEmail, e);
        }
    }

    /**
     * Gửi email chào mừng kèm thông tin tài khoản khi admin tạo mới.
     * @param toEmail   Email người nhận
     * @param fullName  Họ tên cư dân / nhân viên
     * @param username  Username đăng nhập
     * @param password  Mật khẩu ban đầu (plaintext, chỉ gửi 1 lần)
     * @param userType  "cư dân" hoặc "nhân viên"
     */
    public void sendWelcomeEmail(String toEmail, String fullName,
                                 String username, String password, String userType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Chào mừng bạn đến với CondoCare – Thông tin tài khoản");
            message.setText(
                    "Xin chào " + fullName + ",\n\n" +
                            "Ban quản lý đã tạo tài khoản " + userType + " cho bạn trên hệ thống CondoCare.\n\n" +
                            "─────────────────────────────\n" +
                            "  Tên đăng nhập : " + username  + "\n" +
                            "  Mật khẩu      : " + password  + "\n" +
                            "─────────────────────────────\n\n" +
                            "⚠ Vui lòng đăng nhập và đổi mật khẩu ngay sau lần đầu sử dụng.\n\n" +
                            "Truy cập hệ thống tại: http://localhost:8080/login\n\n" +
                            "Trân trọng,\n" +
                            "Ban quản lý CondoCare"
            );

            mailSender.send(message);
            logger.info("Welcome email sent to {} ({})", toEmail, username);
        } catch (Exception e) {
            // Không throw — tạo tài khoản thành công nhưng email lỗi không nên rollback
            logger.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}