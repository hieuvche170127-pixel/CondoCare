package com.swp391.condocare_swp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ─── RESET PASSWORD ───────────────────────────────────────────────────────

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            String resetLink = "http://localhost:8080/reset-password?token=" + resetToken;
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("CondoCare — Đặt lại mật khẩu");
            msg.setText(
                    "Xin chào,\n\n" +
                            "Bạn đã yêu cầu đặt lại mật khẩu.\n\n" +
                            "Nhấn vào link sau để đặt lại (hiệu lực 1 giờ):\n" +
                            resetLink + "\n\n" +
                            "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n" +
                            "Trân trọng,\nBan quản lý CondoCare");
            mailSender.send(msg);
            logger.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Error sending password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }

    public void sendPasswordResetSuccessEmail(String toEmail) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("CondoCare — Đặt lại mật khẩu thành công");
            msg.setText(
                    "Mật khẩu của bạn đã được đặt lại thành công.\n\n" +
                            "Nếu bạn không thực hiện, hãy liên hệ ban quản lý ngay.\n\n" +
                            "Trân trọng,\nBan quản lý CondoCare");
            mailSender.send(msg);
        } catch (Exception e) {
            logger.error("Error sending password reset success email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── WELCOME (Manager tạo tài khoản trực tiếp) ───────────────────────────

    public void sendWelcomeEmail(String toEmail, String fullName,
                                 String username, String password, String userType) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("CondoCare — Thông tin tài khoản của bạn");
            msg.setText(
                    "Xin chào " + fullName + ",\n\n" +
                            "Ban quản lý đã tạo tài khoản " + userType + " cho bạn trên hệ thống CondoCare.\n\n" +
                            "─────────────────────────────\n" +
                            "  Tên đăng nhập : " + username + "\n" +
                            "  Mật khẩu      : " + password + "\n" +
                            "─────────────────────────────\n\n" +
                            "⚠ Vui lòng đăng nhập và đổi mật khẩu ngay sau lần đầu sử dụng.\n\n" +
                            "Truy cập: http://localhost:8080/login\n\n" +
                            "Trân trọng,\nBan quản lý CondoCare");
            mailSender.send(msg);
            logger.info("Welcome email sent to {} ({})", toEmail, username);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── ĐĂNG KÝ MỚI (PENDING) ────────────────────────────────────────────────

    /**
     * [FIX #9] Gửi email xác nhận đăng ký ngay khi resident tự đăng ký thành công.
     * Trước đây AuthService không gửi email nào — cư dân không biết đơn đã được nhận.
     *
     * @param toEmail   Email cư dân vừa đăng ký
     * @param fullName  Tên đầy đủ cư dân
     * @param username  Username đã chọn
     */
    public void sendPendingRegistrationEmail(String toEmail, String fullName, String username) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("CondoCare — Đăng ký tài khoản thành công, chờ xét duyệt");
            msg.setText(
                    "Xin chào " + fullName + ",\n\n" +
                            "Hệ thống CondoCare đã nhận được đơn đăng ký tài khoản của bạn.\n\n" +
                            "Thông tin đăng ký:\n" +
                            "  • Tên đăng nhập: " + username + "\n\n" +
                            "Tài khoản đang được Ban quản lý xem xét và xác minh thông tin.\n" +
                            "Bạn sẽ nhận được email thông báo kết quả trong thời gian sớm nhất.\n\n" +
                            "Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email này.\n\n" +
                            "Trân trọng,\nBan quản lý CondoCare");
            mailSender.send(msg);
            logger.info("Pending registration email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send pending registration email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── DUYỆT TÀI KHOẢN PENDING ─────────────────────────────────────────────

    public void sendAccountApprovedEmail(String toEmail, String fullName) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("CondoCare — Tài khoản của bạn đã được kích hoạt!");
            msg.setText(
                    "Xin chào " + fullName + ",\n\n" +
                            "Tài khoản của bạn đã được Ban quản lý xác minh và kích hoạt thành công.\n\n" +
                            "Bạn có thể đăng nhập ngay tại:\n" +
                            "http://localhost:8080/login\n\n" +
                            "Sau khi đăng nhập, bạn có thể:\n" +
                            "  • Xem thông tin căn hộ\n" +
                            "  • Xem hóa đơn phí dịch vụ\n" +
                            "  • Đăng ký chỗ gửi xe\n" +
                            "  • Gửi yêu cầu hỗ trợ kỹ thuật\n" +
                            "  • Nhận thông báo từ Ban quản lý\n\n" +
                            "Trân trọng,\nBan quản lý CondoCare");
            mailSender.send(msg);
            logger.info("Account approved email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send account approved email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendAccountRejectedEmail(String toEmail, String fullName, String reason) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("CondoCare — Tài khoản của bạn chưa được xác minh");
            msg.setText(
                    "Xin chào " + fullName + ",\n\n" +
                            "Rất tiếc, Ban quản lý chưa thể xác minh tài khoản của bạn.\n\n" +
                            "Lý do: " + (reason != null ? reason : "Thông tin chưa hợp lệ.") + "\n\n" +
                            "Nếu bạn có thắc mắc, vui lòng liên hệ trực tiếp Ban quản lý tòa nhà.\n\n" +
                            "Trân trọng,\nBan quản lý CondoCare");
            mailSender.send(msg);
            logger.info("Account rejected email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send account rejected email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── YÊU CẦU HỖ TRỢ ─────────────────────────────────────────────────────

    /**
     * Gửi email cho cư dân khi yêu cầu hỗ trợ hoàn thành (DONE).
     * Cư dân cần đăng nhập hệ thống để xác nhận.
     *
     * @param toEmail      Email cư dân
     * @param fullName     Tên đầy đủ cư dân
     * @param requestId    Mã yêu cầu (VD: SR001)
     * @param requestTitle Tiêu đề yêu cầu
     * @param staffName    Tên nhân viên đã xử lý
     * @param note         Ghi chú hoàn thành (có thể null)
     */
    public void sendServiceRequestDoneEmail(String toEmail, String fullName,
                                            String requestId, String requestTitle,
                                            String staffName, String note) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail);
            msg.setSubject("CondoCare — Yêu cầu hỗ trợ của bạn đã được hoàn thành");
            msg.setText(
                    "Xin chào " + fullName + ",\n\n" +
                            "Yêu cầu hỗ trợ của bạn đã được xử lý xong. Thông tin chi tiết:\n\n" +
                            "─────────────────────────────\n" +
                            "  Mã yêu cầu   : " + requestId + "\n" +
                            "  Tiêu đề      : " + requestTitle + "\n" +
                            "  Nhân viên    : " + (staffName != null ? staffName : "Ban quản lý") + "\n" +
                            (note != null && !note.isBlank()
                                    ? "  Ghi chú      : " + note + "\n"
                                    : "") +
                            "─────────────────────────────\n\n" +
                            "✅ Vui lòng đăng nhập hệ thống để xem ảnh xác nhận và xác nhận hoàn thành:\n" +
                            "http://localhost:8080/resident/requests\n\n" +
                            "Nếu bạn chưa hài lòng với kết quả xử lý, vui lòng liên hệ trực tiếp\n" +
                            "Ban quản lý tòa nhà để được hỗ trợ thêm.\n\n" +
                            "Trân trọng,\nBan quản lý CondoCare");
            mailSender.send(msg);
            logger.info("Service request done email sent to {} for request {}", toEmail, requestId);
        } catch (Exception e) {
            // Không throw — lỗi email không được làm gián đoạn luồng chính
            logger.error("Failed to send service request done email to {} (SR: {}): {}",
                    toEmail, requestId, e.getMessage());
        }
    }
}