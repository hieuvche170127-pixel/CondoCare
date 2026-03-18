package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public Residents getResidentById(String id){
        return residentRepository.findResidentsById(id);
    }

    public void saveResident(Residents residents) {
        residentRepository.save(residents);
    }

    public Page<Residents> getResidentPaginated(String keyword, int pageNo, int pageSize) {

        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);

        if (keyword != null && !keyword.trim().isEmpty()) {
            return residentRepository.searchByKeyword(keyword.trim(), pageable);
        }
        return residentRepository.findAll(pageable);
    }

}