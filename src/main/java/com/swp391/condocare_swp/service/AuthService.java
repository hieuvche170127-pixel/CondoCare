package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.*;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import com.swp391.condocare_swp.security.CustomUserDetailsService;
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

/**
 * Service xử lý authentication và authorization
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final Set<String> VALID_STAFF_ROLE_IDS = Set.of("R001", "R002", "R003");

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ResidentsRepository residentsRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private EmailService emailService;

//    @Value("${password.reset.token.expiration}")
//    private long resetTokenExpiration;

    /**
     * Đăng nhập
     */
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt - username: {}, userType: {}",
                loginRequest.getUsernameOrEmail(),
                loginRequest.getUserType());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        String username = authentication.getName();

        logger.info("Authentication successful for: {}", username);

        if ("staff".equals(loginRequest.getUserType())) {
            // Nếu User chọn Login vào staff
            logger.info("Validating Staff access for: {}", username);

            Staff staff = staffRepository.findByUsernameOrEmail(username, username).orElse(null);
            if (staff == null) {
                // User hợp lệ nhưng không tồn tại trong staff table
                // -> Resident đang cố gắng truy cập staff
                logger.warn("SECURITY: Resident '{}' attempted to access Staff portal", username);
                throw new RuntimeException("Tài khoản này không có quyền truy cập vào hệ thống quản lý. Vui lòng chọn 'Cư dân' để đăng nhập.");
            }

            // Kiểm tra tài khoản nhân viên bị vô hiệu hóa hoặc đã nghỉ
            if (staff.getStatus() == Staff.StaffStatus.RESIGNED) {
                logger.warn("SECURITY: Resigned staff '{}' attempted to login", username);
                throw new RuntimeException("Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
            }
            if (staff.getStatus() == Staff.StaffStatus.ON_LEAVE) {
                logger.warn("Staff '{}' on leave attempted to login", username);
                throw new RuntimeException("Tài khoản của bạn đang trong trạng thái nghỉ phép. Vui lòng liên hệ quản trị viên.");
            }

            // Validate Role (chỉ cho phép ADMIN, MANAGER, STAFF)
            String roleId = staff.getRole().getId();
            if (!VALID_STAFF_ROLE_IDS.contains(roleId)) {
                logger.warn("SECURITY: Invalid role_id '{}' for {}", roleId, username);
                throw new RuntimeException("Vai trò không hợp lệ. Vui lòng liên hệ admin.");
            }

            // Update last login
            staff.setLastLogin(LocalDateTime.now());
            staffRepository.save(staff);

            logger.info("Staff login successful - ID: {}, Role ID: {}", staff.getId(), roleId);

            return new AuthResponse(
                    jwt,
                    staff.getId(),
                    staff.getUsername(),
                    staff.getEmail(),
                    staff.getFullName(),
                    staff.getRole().getName(),
                    "staff"
            );
        } else if ("resident".equalsIgnoreCase(loginRequest.getUserType())) {
            // Nếu User chọn Login vào resident
            logger.info("Validating Resident access for: {}", username);

            Residents resident = residentsRepository.findByUsernameOrEmail(username, username).orElse(null);

            if (resident == null) {
                // Username hợp lệ NHƯNG không tồn tại trong Residents table
                // → Đây là Staff cố gắng truy cập Resident portal
                logger.warn("SECURITY: Staff '{}' attempted to access Resident portal", username);
                throw new RuntimeException("Tài khoản này không phải tài khoản cư dân. Vui lòng chọn 'Nhân viên' để đăng nhập.");
            }

            // Kiểm tra tài khoản bị vô hiệu hóa
            if (resident.getStatus() == Residents.ResidentStatus.INACTIVE) {
                logger.warn("SECURITY: Inactive resident '{}' attempted to login", username);
                throw new RuntimeException("Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ ban quản lý.");
            }

            // Update last login
            resident.setLastLogin(LocalDateTime.now());
            residentsRepository.save(resident);

            logger.info("Resident login successful - ID: {}, Type: {}",
                    resident.getId(), resident.getType());

            return new AuthResponse(
                    jwt,
                    resident.getId(),
                    resident.getUsername(),
                    resident.getEmail(),
                    resident.getFullName(),
                    resident.getType().name(),
                    "resident"
            );

        } else {
            logger.error("Invalid userType: {}", loginRequest.getUserType());
            throw new RuntimeException("Loại người dùng không hợp lệ. Vui lòng chọn 'Nhân viên' hoặc 'Cư dân'.");
        }
    }

    /**
     * Đăng ký resident mới - chỉ cần thông tin cơ bản
     * Admin sẽ phê duyệt & gán căn hộ sau
     */
    @Transactional
    public String register(RegisterRequest registerRequest) {
        // Validate password match
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu và xác nhận mật khẩu không khớp");
        }

        // Check username exists (cả Staff và Resident)
        if (residentsRepository.existsByUsername(registerRequest.getUsername())
                || staffRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username đã tồn tại, vui lòng chọn username khác");
        }

        // Check email exists
        if (residentsRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Tạo Resident mới với thông tin cơ bản
        Residents resident = new Residents();
        resident.setId(generateResidentId());
        resident.setUsername(registerRequest.getUsername());
        resident.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        resident.setFullName(registerRequest.getFullName());
        resident.setEmail(registerRequest.getEmail());
        resident.setPhone(registerRequest.getPhone());
        // Các trường optional để null, Admin điền sau
        resident.setIdNumber(null);
        resident.setApartment(null);
        resident.setType(Residents.ResidentType.TENANT);      // Mặc định
        resident.setGender(Residents.Gender.M);               // Mặc định
        resident.setStatus(Residents.ResidentStatus.ACTIVE);

        residentsRepository.save(resident);

        return "Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.";
    }

    /**
     * Quên mật khẩu - Gửi email reset
     */
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String resetToken;

        // Tìm user theo email
        if ("staff".equalsIgnoreCase(request.getUserType())) {
            Staff staff = staffRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này"));

            // Tạo JWT reset token chứa username
            resetToken = tokenProvider.generateTokenFromUsername(staff.getUsername());

            // Gửi email
            emailService.sendPasswordResetEmail(staff.getEmail(), resetToken);

        } else {
            Residents resident = residentsRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này"));

            // Tạo JWT reset token chứa username
            resetToken = tokenProvider.generateTokenFromUsername(resident.getUsername());

            // Gửi email
            emailService.sendPasswordResetEmail(resident.getEmail(), resetToken);
        }

        return "Email reset password đã được gửi. Vui lòng kiểm tra email của bạn.";
    }

    /**
     * Reset password
     */
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        // Validate password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password mới và Confirm Password không khớp");
        }

        // Validate token (cần implement logic validate token)
        // Đây là simplified version

        // Get username from token
        String username = tokenProvider.getUsernameFromToken(request.getToken());

        // Try to find in Staff
        Staff staff = staffRepository.findByUsername(username).orElse(null);

        if (staff != null) {
            staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
            staffRepository.save(staff);
            emailService.sendPasswordResetSuccessEmail(staff.getEmail());
        } else {
            Residents resident = residentsRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            resident.setPassword(passwordEncoder.encode(request.getNewPassword()));
            residentsRepository.save(resident);
            emailService.sendPasswordResetSuccessEmail(resident.getEmail());
        }

        return "Reset password thành công! Bạn có thể đăng nhập với password mới.";
    }

    /**
     * Generate unique Resident ID
     */
    private String generateResidentId() {
        // Format: R + timestamp (10 digits)
        return "R" + System.currentTimeMillis() % 1000000000;
    }
}