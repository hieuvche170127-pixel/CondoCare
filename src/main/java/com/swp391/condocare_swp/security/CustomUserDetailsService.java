package com.swp391.condocare_swp.security;

import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service để load user details từ database
 * Implement UserDetailsService của Spring Security
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private StaffRepository staffRepository;
    
    @Autowired
    private ResidentsRepository residentsRepository;
    
    /**
     * Load user by username
     * Tìm trong cả Staff và Residents
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Thử tìm trong Staff trước
        Staff staff = staffRepository.findByUsername(username).orElse(null);
        
        if (staff != null) {
            // Nếu tìm thấy Staff, tạo UserDetails cho Staff
            return buildUserDetailsFromStaff(staff);
        }
        
        // Nếu không tìm thấy Staff, tìm trong Residents
        Residents resident = residentsRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));
        
        // Tạo UserDetails cho Resident
        return buildUserDetailsFromResident(resident);
    }
    
    /**
     * Load user by username hoặc email
     */
    public UserDetails loadUserByUsernameOrEmail(String usernameOrEmail) throws UsernameNotFoundException {
        // Thử tìm trong Staff
        Staff staff = staffRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElse(null);
        
        if (staff != null) {
            return buildUserDetailsFromStaff(staff);
        }
        
        // Tìm trong Residents
        Residents resident = residentsRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));
        
        return buildUserDetailsFromResident(resident);
    }
    
    /**
     * Tạo UserDetails từ Staff
     */
    private UserDetails buildUserDetailsFromStaff(Staff staff) {
        // Tạo list authorities từ Role
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + staff.getRole().getName()));
        
        // Return Spring Security User object
        return User.builder()
                .username(staff.getUsername())
                .password(staff.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(staff.getStatus() != Staff.StaffStatus.ACTIVE)
                .credentialsExpired(false)
                .disabled(staff.getStatus() != Staff.StaffStatus.ACTIVE)
                .build();
    }
    
    /**
     * Tạo UserDetails từ Resident
     */
    private UserDetails buildUserDetailsFromResident(Residents resident) {
        // Tạo list authorities từ Type
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + resident.getType().name()));
        
        return User.builder()
                .username(resident.getUsername())
                .password(resident.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(resident.getStatus() != Residents.ResidentStatus.ACTIVE)
                .credentialsExpired(false)
                .disabled(resident.getStatus() != Residents.ResidentStatus.ACTIVE)
                .build();
    }
}
