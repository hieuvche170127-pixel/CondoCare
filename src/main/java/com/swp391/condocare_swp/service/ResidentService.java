package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentsRepository residentRepository;


    // Hàm lấy toàn bộ danh sách cư dân để đổ vào thẻ <select>
    public List<Residents> getAllResidents() {
        return residentRepository.findAll();
    }
}