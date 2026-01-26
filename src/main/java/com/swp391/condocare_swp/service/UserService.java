package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.UserDto;
import com.swp391.condocare_swp.entity.User;
import com.swp391.condocare_swp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserDto getCurrentUserDto(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        UserDto dto = new UserDto();
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setRoleId(user.getRole().getId());
        return dto;
    }
}