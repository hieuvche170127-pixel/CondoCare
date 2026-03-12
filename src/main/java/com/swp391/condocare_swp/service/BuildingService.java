package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
