package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingService {
    private final BuildingRepository buildingRepository;
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateRandomId() {
        StringBuilder sb = new StringBuilder("BLD");
        for (int i = 0; i <7; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }


    public List<Building> getAllBuildingList() {
        return buildingRepository.findAll();
    }

    public Building getBuildingById(String id){
        return buildingRepository.findBuildingById(id);
    }

    public Page<Building> getBuildingsPaginated(String keyword, int pageNo, int pageSize) {

        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);

        if (keyword != null && !keyword.trim().isEmpty()) {
            return buildingRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }
        return buildingRepository.findAll(pageable);
    }

    public List<Building> getBuildings(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return buildingRepository.findByNameContainingIgnoreCase(keyword.trim());
        }
        return buildingRepository.findAll();
    }

    // Thêm hoặc Cập nhật tòa nhà
    public void saveBuilding(Building building) {
        if (building.getId() == null || building.getId().trim().isEmpty()) {
            String newId;
            boolean isUnique = false;

            while (!isUnique) {
                newId = generateRandomId();
                if (!buildingRepository.existsById(newId)) {
                    building.setId(newId);
                    isUnique = true;
                }
            }
        }

        buildingRepository.save(building);
    }

    // Thay đổi trạng thái qua lại (ACTIVE <-> INACTIVE)
    public void toggleBuildingStatus(String id) {
        Building building = getBuildingById(id);
        if (building != null) {
            // Kiểm tra trạng thái hiện tại và đảo ngược lại
            if (building.getStatus() == Building.BuildingStatus.ACTIVE) {
                building.setStatus(Building.BuildingStatus.INACTIVE);
            } else {
                building.setStatus(Building.BuildingStatus.ACTIVE);
            }
            buildingRepository.save(building);
        }
    }

}
