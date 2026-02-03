package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.*;
import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import com.swp391.condocare_swp.security.CustomUserDetailsService;
import com.swp391.condocare_swp.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service xử lý authentication và authorization
 */
@Service
public class AuthService {
    
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
    
    @Value("${password.reset.token.expiration}")
    private long resetTokenExpiration;
    
    /**
     * Đăng nhập
     */
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );
        
        // Set authentication vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Tạo JWT token
        String jwt = tokenProvider.generateToken(authentication);
        
        // Lấy thông tin user
        String username = authentication.getName();
        
        // Kiểm tra user type và lấy thông tin tương ứng
        if ("staff".equalsIgnoreCase(loginRequest.getUserType())) {
            Staff staff = staffRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException("Staff not found"));
            
            // Update last login
            staff.setLastLogin(LocalDateTime.now());
            staffRepository.save(staff);
            
            return new AuthResponse(
                    jwt,
                    staff.getId(),
                    staff.getUsername(),
                    staff.getEmail(),
                    staff.getFullName(),
                    staff.getRole().getName(),
                    "staff"
            );
        } else {
            Residents resident = residentsRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException("Resident not found"));
            
            // Update last login
            resident.setLastLogin(LocalDateTime.now());
            residentsRepository.save(resident);
            
            return new AuthResponse(
                    jwt,
                    resident.getId(),
                    resident.getUsername(),
                    resident.getEmail(),
                    resident.getFullName(),
                    resident.getType().name(),
                    "resident"
            );
        }
    }
    
    /**
     * Đăng ký resident mới
     */
    @Transactional
    public String register(RegisterRequest registerRequest) {
        // Validate password match
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("Password và Confirm Password không khớp");
        }
        
        // Check username exists
        if (residentsRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        
        // Check email exists
        if (residentsRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        
        // Check ID number exists
        if (residentsRepository.existsByIdNumber(registerRequest.getIdNumber())) {
            throw new RuntimeException("Số CMND/CCCD đã được đăng ký");
        }
        
        // Check apartment exists
        Apartment apartment = apartmentRepository.findById(registerRequest.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Căn hộ không tồn tại"));
        
        // Tạo Resident mới
        Residents resident = new Residents();
        resident.setId(generateResidentId());
        resident.setUsername(registerRequest.getUsername());
        resident.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        resident.setFullName(registerRequest.getFullName());
        resident.setEmail(registerRequest.getEmail());
        resident.setPhone(registerRequest.getPhone());
        resident.setIdNumber(registerRequest.getIdNumber());
        resident.setApartment(apartment);
        resident.setType(Residents.ResidentType.valueOf(registerRequest.getResidentType()));
        resident.setGender(Residents.Gender.valueOf(registerRequest.getGender()));
        resident.setStatus(Residents.ResidentStatus.ACTIVE);
        
        residentsRepository.save(resident);
        
        // Update apartment total resident
        apartment.setTotalResident(apartment.getTotalResident() + 1);
        apartmentRepository.save(apartment);
        
        return "Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.";
    }
    
    /**
     * Quên mật khẩu - Gửi email reset
     */
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        // Tìm user theo email
        if ("staff".equalsIgnoreCase(request.getUserType())) {
            Staff staff = staffRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này"));
            
            // Tạo reset token
            String resetToken = UUID.randomUUID().toString();
            
            // Lưu token vào database (có thể tạo bảng riêng hoặc dùng Redis)
            // Ở đây ta sẽ dùng JWT token với expiration time
            
            // Gửi email
            emailService.sendPasswordResetEmail(staff.getEmail(), resetToken);
            
        } else {
            Residents resident = residentsRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này"));
            
            String resetToken = UUID.randomUUID().toString();
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
