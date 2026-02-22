package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.ChangePasswordRequest;
import com.swp391.condocare_swp.dto.ProfileResponse;
import com.swp391.condocare_swp.dto.UpdateProfileRequest;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý các chức năng liên quan đến Profile
 */
@Service
public class ProfileService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    
    @Autowired
    private StaffRepository staffRepository;
    
    @Autowired
    private ResidentsRepository residentsRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Lấy thông tin profile của user hiện tại
     */
    public ProfileResponse getProfile() {
        // Lấy username từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("Getting profile for user: {}", username);
        
        // Tìm user (Staff hoặc Resident)
        Staff staff = staffRepository.findByUsername(username).orElse(null);
        
        if (staff != null) {
            // Nếu là Staff
            return new ProfileResponse(
                    staff.getId(),
                    staff.getUsername(),
                    staff.getFullName(),
                    staff.getEmail(),
                    staff.getPhone(),
                    staff.getPosition(),
                    staff.getDepartment(),
                    null,  // type
                    null,  // apartmentId
                    "staff"
            );
        } else {
            // Nếu là Resident
            Residents resident = residentsRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            return new ProfileResponse(
                    resident.getId(),
                    resident.getUsername(),
                    resident.getFullName(),
                    resident.getEmail(),
                    resident.getPhone(),
                    null,  // position
                    null,  // department
                    resident.getType().name(),
                    resident.getApartment() != null ? resident.getApartment().getId() : null,
                    "resident"
            );
        }
    }
    
    /**
     * Cập nhật thông tin profile
     */
    @Transactional
    public String updateProfile(UpdateProfileRequest request) {
        // Lấy username từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("Updating profile for user: {}", username);
        
        // Tìm user (Staff hoặc Resident)
        Staff staff = staffRepository.findByUsername(username).orElse(null);
        
        if (staff != null) {
            // Update Staff
            
            // Check email đã tồn tại chưa (nếu đổi email)
            if (!staff.getEmail().equals(request.getEmail())) {
                if (staffRepository.existsByEmail(request.getEmail())) {
                    throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");
                }
            }
            
            // Update fields
            staff.setFullName(request.getFullName());
            staff.setEmail(request.getEmail());
            staff.setPhone(request.getPhone());
            
            staffRepository.save(staff);
            
            logger.info("Staff profile updated successfully: {}", username);
            return "Cập nhật thông tin thành công!";
            
        } else {
            // Update Resident
            Residents resident = residentsRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check email đã tồn tại chưa (nếu đổi email)
            if (!resident.getEmail().equals(request.getEmail())) {
                if (residentsRepository.existsByEmail(request.getEmail())) {
                    throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");
                }
            }
            
            // Update fields
            resident.setFullName(request.getFullName());
            resident.setEmail(request.getEmail());
            resident.setPhone(request.getPhone());
            
            residentsRepository.save(resident);
            
            logger.info("Resident profile updated successfully: {}", username);
            return "Cập nhật thông tin thành công!";
        }
    }
    
    /**
     * Đổi mật khẩu
     */
    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        // Validate password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }
        
        // Lấy username từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        logger.info("Changing password for user: {}", username);
        
        // Tìm user (Staff hoặc Resident)
        Staff staff = staffRepository.findByUsername(username).orElse(null);
        
        if (staff != null) {
            // Change password cho Staff
            
            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), staff.getPassword())) {
                throw new RuntimeException("Mật khẩu hiện tại không đúng");
            }
            
            // Update password
            staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
            staffRepository.save(staff);
            
            logger.info("Staff password changed successfully: {}", username);
            return "Đổi mật khẩu thành công!";
            
        } else {
            // Change password cho Resident
            Residents resident = residentsRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), resident.getPassword())) {
                throw new RuntimeException("Mật khẩu hiện tại không đúng");
            }
            
            // Update password
            resident.setPassword(passwordEncoder.encode(request.getNewPassword()));
            residentsRepository.save(resident);
            
            logger.info("Resident password changed successfully: {}", username);
            return "Đổi mật khẩu thành công!";
        }
    }
}
