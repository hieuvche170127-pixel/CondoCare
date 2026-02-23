package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingService {
    private final BuildingRepository buildingRepository;

    public List<Building> getAllBuildingList() {
        return buildingRepository.findAll();
    }

    public Building getBuildingById(String id){
        return buildingRepository.findBuildingById(id);
    }

    public List<Building> getBuildings(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return buildingRepository.findByNameContainingIgnoreCase(keyword.trim());
        }
        return buildingRepository.findAll();
    }

}
