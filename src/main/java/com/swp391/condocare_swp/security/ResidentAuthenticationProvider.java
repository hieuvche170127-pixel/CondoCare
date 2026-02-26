//package com.swp391.condocare_swp.security;
//
//import com.swp391.condocare_swp.entity.Residents;
//import com.swp391.condocare_swp.repository.ResidentsRepository;
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
//public class ResidentAuthenticationProvider implements AuthenticationProvider {
//
//    private final ResidentsRepository residentsRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Autowired
//    public ResidentAuthenticationProvider(ResidentsRepository residentsRepository, PasswordEncoder passwordEncoder) {
//        this.residentsRepository = residentsRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//        String usernameOrEmail = authentication.getName();
//        String password = authentication.getCredentials().toString();
//
//        Residents resident = residentsRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
//                .orElseThrow(() -> new BadCredentialsException("Tài khoản hoặc mật khẩu không đúng cho cư dân"));
//
//        if (!passwordEncoder.matches(password, resident.getPassword())) {
//            throw new BadCredentialsException("Tài khoản hoặc mật khẩu không đúng cho cư dân");
//        }
//
//        // Resident có role dựa trên type
//        String role = "ROLE_RESIDENT_" + resident.getType().name().toUpperCase();
//        UserDetails userDetails = new User(
//                resident.getUsername(),
//                resident.getPassword(),
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