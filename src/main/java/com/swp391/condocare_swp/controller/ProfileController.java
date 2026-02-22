package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.ChangePasswordRequest;
import com.swp391.condocare_swp.dto.ProfileResponse;
import com.swp391.condocare_swp.dto.UpdateProfileRequest;
import com.swp391.condocare_swp.service.ProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý các API liên quan đến Profile
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProfileController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    
    @Autowired
    private ProfileService profileService;
    
    /**
     * API lấy thông tin profile của user hiện tại
     * GET /api/profile/me
     * 
     * @return ProfileResponse chứa thông tin user
     */
    @GetMapping("/me")
    public ResponseEntity<?> getProfile() {
        try {
            logger.info("GET /api/profile/me - Get current user profile");
            
            ProfileResponse profile = profileService.getProfile();
            
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error getting profile: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * API cập nhật thông tin profile
     * PUT /api/profile/update
     * 
     * @param request UpdateProfileRequest chứa thông tin cần update
     * @return Success message
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            logger.info("PUT /api/profile/update - Update profile");
            logger.debug("Update profile request: {}", request);
            
            String message = profileService.updateProfile(request);
            
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error updating profile: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * API đổi mật khẩu
     * PUT /api/profile/change-password
     * 
     * @param request ChangePasswordRequest chứa current password và new password
     * @return Success message
     */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            logger.info("PUT /api/profile/change-password - Change password");
            
            String message = profileService.changePassword(request);
            
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error changing password: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
