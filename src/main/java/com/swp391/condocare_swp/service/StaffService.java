package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final StaffRepository staffRepository;

    public List<Staff> getAllStaffs() {
        return staffRepository.findAll();
    }
}