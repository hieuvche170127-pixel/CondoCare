package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.DashboardStats;
import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Invoice;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.ServiceRequest;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.BuildingRepository;
import com.swp391.condocare_swp.repository.InvoiceRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.ServiceRequestRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service xử lý logic cho Dashboard
 */
@Service
public class DashboardService {

    @Autowired private ApartmentRepository      apartmentRepository;
    @Autowired private ResidentsRepository      residentsRepository;
    @Autowired private StaffRepository          staffRepository;
    @Autowired private BuildingRepository       buildingRepository;
    @Autowired private InvoiceRepository        invoiceRepository;
    @Autowired private ServiceRequestRepository serviceRequestRepository;

    /**
     * Lấy thống kê cho Dashboard
     */
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        // ── Căn hộ ──────────────────────────────────────────
        stats.setTotalApartments(apartmentRepository.count());
        stats.setEmptyApartments(apartmentRepository.countByStatus(Apartment.ApartmentStatus.EMPTY));
        stats.setOccupiedApartments(apartmentRepository.countByStatus(Apartment.ApartmentStatus.OCCUPIED));
        stats.setMaintenanceApartments(apartmentRepository.countByStatus(Apartment.ApartmentStatus.MAINTENANCE));

        // ── Cư dân ──────────────────────────────────────────
        stats.setTotalResidents(residentsRepository.count());
        stats.setActiveResidents(residentsRepository.countByStatus(Residents.ResidentStatus.ACTIVE));

        // ── Nhân viên ───────────────────────────────────────
        stats.setTotalStaff(staffRepository.count());
        stats.setActiveStaff(staffRepository.countByStatus(Staff.StaffStatus.ACTIVE));

        // ── Tòa nhà ─────────────────────────────────────────
        stats.setTotalBuildings(buildingRepository.count());

        // ── Hóa đơn ─────────────────────────────────────────
        stats.setPaidInvoices(invoiceRepository.countByStatus(Invoice.InvoiceStatus.PAID));
        stats.setUnpaidInvoices(invoiceRepository.countByStatus(Invoice.InvoiceStatus.UNPAID));
        stats.setOverdueInvoices(invoiceRepository.countByStatus(Invoice.InvoiceStatus.OVERDUE));

        // ── Yêu cầu hỗ trợ ──────────────────────────────────
        stats.setPendingRequests(serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.PENDING));
        stats.setInProgressRequests(serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.IN_PROGRESS));
        stats.setDoneRequests(serviceRequestRepository.countByStatus(ServiceRequest.RequestStatus.DONE));

        return stats;
    }
}