//package com.swp391.condocare_swp.security;
//
//import com.swp391.condocare_swp.entity.Staff;
//import com.swp391.condocare_swp.repository.StaffRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.Collections;
//
//@Component
//public class StaffAuthenticationProvider implements AuthenticationProvider {
//
//    private final StaffRepository staffRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Autowired
//    public StaffAuthenticationProvider(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
//        this.staffRepository = staffRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//        String usernameOrEmail = authentication.getName();
//        String password = authentication.getCredentials().toString();
//
//        Staff staff = staffRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
//                .orElseThrow(() -> new BadCredentialsException("Tài khoản hoặc mật khẩu không đúng cho nhân viên"));
//
//        if (!passwordEncoder.matches(password, staff.getPassword())) {
//            throw new BadCredentialsException("Tài khoản hoặc mật khẩu không đúng cho nhân viên");
//        }
//
//        // Tạo UserDetails với role
//        String role = "ROLE_" + staff.getRole().getName().toUpperCase();
//        UserDetails userDetails = new User(
//                staff.getUsername(),
//                staff.getPassword(),
//                Collections.singletonList(new SimpleGrantedAuthority(role))
//        );
//
//        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
//    }
//
//    @Override
//    public boolean supports(Class<?> authentication) {
//        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
//    }
//}