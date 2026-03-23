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
        // Tìm Staff theo username HOẶC email
        Staff staff = staffRepository.findByUsernameOrEmail(username, username).orElse(null);
        if (staff != null) {
            return buildUserDetailsFromStaff(staff);
        }

        // Tìm Resident theo username HOẶC email
        Residents resident = residentsRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

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
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + staff.getRole().getName()));

        return User.builder()
                .username(staff.getUsername())
                .password(staff.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                // Chỉ lock khi RESIGNED — ON_LEAVE vẫn cho authenticate,
                // AuthService sẽ kiểm tra và trả về message rõ ràng hơn
                .accountLocked(staff.getStatus() == Staff.StaffStatus.RESIGNED)
                .credentialsExpired(false)
                .disabled(staff.getStatus() == Staff.StaffStatus.RESIGNED)
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
