package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.DashboardStats;
import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.BuildingRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service xử lý logic cho Dashboard
 */
@Service
public class DashboardService {
    
    @Autowired
    private ApartmentRepository apartmentRepository;
    
    @Autowired
    private ResidentsRepository residentsRepository;
    
    @Autowired
    private StaffRepository staffRepository;
    
    @Autowired
    private BuildingRepository buildingRepository;
    
    /**
     * Lấy thống kê cho Dashboard
     */
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        // Thống kê Apartment
        stats.setTotalApartments(apartmentRepository.count());
        stats.setEmptyApartments(apartmentRepository.countByStatus(Apartment.ApartmentStatus.EMPTY));
        stats.setOccupiedApartments(apartmentRepository.countByStatus(Apartment.ApartmentStatus.OCCUPIED));
        stats.setMaintenanceApartments(apartmentRepository.countByStatus(Apartment.ApartmentStatus.MAINTENANCE));
        
        // Thống kê Residents
        stats.setTotalResidents(residentsRepository.count());
        stats.setActiveResidents(residentsRepository.countByStatus(Residents.ResidentStatus.ACTIVE));
        
        // Thống kê Staff
        stats.setTotalStaff(staffRepository.count());
        stats.setActiveStaff(staffRepository.countByStatus(Staff.StaffStatus.ACTIVE));
        
        // Thống kê Building
        stats.setTotalBuildings(buildingRepository.count());
        
        return stats;
    }
}
