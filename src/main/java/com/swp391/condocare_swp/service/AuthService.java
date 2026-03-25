package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.*;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import com.swp391.condocare_swp.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final Set<String> VALID_STAFF_ROLE_IDS = Set.of("ROLE001", "ROLE002", "ROLE003", "ROLE004");

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private StaffRepository staffRepository;
    @Autowired private ResidentsRepository residentsRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private EmailService emailService;

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt — username: {}, userType: {}",
                loginRequest.getUsernameOrEmail(), loginRequest.getUserType());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt      = tokenProvider.generateToken(authentication);
        String username = authentication.getName();

        if ("staff".equalsIgnoreCase(loginRequest.getUserType())) {
            Staff staff = staffRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException(
                            "Tài khoản này không có quyền truy cập vào hệ thống quản lý. " +
                                    "Vui lòng chọn 'Cư dân' để đăng nhập."));

            if (staff.getStatus() == Staff.StaffStatus.RESIGNED)
                throw new RuntimeException("Tài khoản của bạn đã bị vô hiệu hóa. Liên hệ quản trị viên.");
            if (staff.getStatus() == Staff.StaffStatus.ON_LEAVE)
                throw new RuntimeException("Tài khoản đang trong trạng thái nghỉ phép. Liên hệ quản trị viên.");

            staff.setLastLogin(LocalDateTime.now());
            staffRepository.save(staff);
            logger.info("Staff login OK — ID: {}, Role: {}", staff.getId(), staff.getRole().getName());

            return new AuthResponse(jwt, staff.getId(), staff.getUsername(),
                    staff.getEmail(), staff.getFullName(), staff.getRole().getName(), "staff");

        } else if ("resident".equalsIgnoreCase(loginRequest.getUserType())) {
            Residents resident = residentsRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException(
                            "Tài khoản này không phải tài khoản cư dân. " +
                                    "Vui lòng chọn 'Nhân viên' để đăng nhập."));

            // ── KIỂM TRA TRẠNG THÁI ──────────────────────────────────────────
            if (resident.getStatus() == Residents.ResidentStatus.PENDING)
                throw new RuntimeException(
                        "Tài khoản của bạn đang chờ Manager xác minh. " +
                                "Vui lòng chờ email thông báo kích hoạt.");
            if (resident.getStatus() == Residents.ResidentStatus.INACTIVE)
                throw new RuntimeException("Tài khoản đã bị vô hiệu hóa. Liên hệ ban quản lý.");

            resident.setLastLogin(LocalDateTime.now());
            residentsRepository.save(resident);
            logger.info("Resident login OK — ID: {}, Type: {}", resident.getId(), resident.getType());

            return new AuthResponse(jwt, resident.getId(), resident.getUsername(),
                    resident.getEmail(), resident.getFullName(),
                    resident.getType().name(), "resident");

        } else {
            throw new RuntimeException("Loại người dùng không hợp lệ. Chọn 'Nhân viên' hoặc 'Cư dân'.");
        }
    }

    // ─── REGISTER ─────────────────────────────────────────────────────────────

    /**
     * Resident tự đăng ký.
     * Tài khoản sẽ ở trạng thái PENDING cho đến khi Manager duyệt.
     * Yêu cầu: username, password, fullName, phone, email.
     * Optional: idNumber (CCCD — dùng để Manager xác minh danh tính).
     */
    @Transactional
    public String register(RegisterRequest registerRequest) {
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword()))
            throw new RuntimeException("Mật khẩu và xác nhận mật khẩu không khớp.");

        if (residentsRepository.existsByUsername(registerRequest.getUsername())
                || staffRepository.existsByUsername(registerRequest.getUsername()))
            throw new RuntimeException("Username đã tồn tại, vui lòng chọn username khác.");

        if (registerRequest.getEmail() != null && !registerRequest.getEmail().isBlank()
                && residentsRepository.existsByEmail(registerRequest.getEmail()))
            throw new RuntimeException("Email đã được sử dụng.");

        if (registerRequest.getPhone() != null && !registerRequest.getPhone().isBlank()
                && residentsRepository.existsByPhone(registerRequest.getPhone()))
            throw new RuntimeException("Số điện thoại đã được sử dụng.");

        Residents resident = new Residents();
        resident.setId(generateResidentId());
        resident.setUsername(registerRequest.getUsername().trim());
        resident.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        resident.setFullName(registerRequest.getFullName());
        resident.setEmail(registerRequest.getEmail());
        resident.setPhone(registerRequest.getPhone());
        resident.setType(Residents.ResidentType.TENANT);   // default — Manager sẽ cập nhật sau
        resident.setGender(Residents.Gender.M);            // default — cập nhật ở profile
        resident.setIdNumber(registerRequest.getIdNumber()); // CCCD để Manager xác minh
        resident.setApartment(null);                       // Manager gán sau khi duyệt
        resident.setStatus(Residents.ResidentStatus.PENDING); // ← PENDING, không phải ACTIVE

        residentsRepository.save(resident);
        logger.info("New resident registered (PENDING): {} — {}", resident.getId(), resident.getUsername());

        return "Đăng ký thành công! Tài khoản của bạn đang chờ Manager xác minh. " +
                "Bạn sẽ nhận được email thông báo sau khi được kích hoạt.";
    }

    // ─── FORGOT PASSWORD ──────────────────────────────────────────────────────

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String resetToken;
        if ("staff".equalsIgnoreCase(request.getUserType())) {
            Staff staff = staffRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này."));
            resetToken = tokenProvider.generateTokenFromUsername(staff.getUsername());
            emailService.sendPasswordResetEmail(staff.getEmail(), resetToken);
        } else {
            Residents resident = residentsRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này."));
            resetToken = tokenProvider.generateTokenFromUsername(resident.getUsername());
            emailService.sendPasswordResetEmail(resident.getEmail(), resetToken);
        }
        return "Email reset password đã được gửi. Vui lòng kiểm tra hộp thư.";
    }

    // ─── RESET PASSWORD ───────────────────────────────────────────────────────

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new RuntimeException("Password mới và Confirm Password không khớp.");

        String username = tokenProvider.getUsernameFromToken(request.getToken());
        Staff staff = staffRepository.findByUsername(username).orElse(null);

        if (staff != null) {
            staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
            staffRepository.save(staff);
            emailService.sendPasswordResetSuccessEmail(staff.getEmail());
        } else {
            Residents resident = residentsRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
            resident.setPassword(passwordEncoder.encode(request.getNewPassword()));
            residentsRepository.save(resident);
            emailService.sendPasswordResetSuccessEmail(resident.getEmail());
        }
        return "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.";
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private String generateResidentId() {
        for (int i = 1; i <= 9999; i++) {
            String candidate = "RES" + String.format("%03d", i);
            if (!residentsRepository.existsById(candidate)) return candidate;
        }
        return "RES" + (System.currentTimeMillis() % 100000L);
    }
}