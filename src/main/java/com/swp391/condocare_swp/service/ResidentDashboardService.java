package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service tổng hợp cho toàn bộ Resident Dashboard.
 * Mọi method đều tự lấy username từ JWT (SecurityContext)
 * → Cư dân chỉ nhìn thấy dữ liệu của chính mình.
 */
@Service
public class ResidentDashboardService {

    @Autowired private ResidentsRepository    residentsRepo;
    @Autowired private NotificationRepository notifRepo;
    @Autowired private ServiceRequestRepository srRepo;
    @Autowired private InvoiceRepository      invoiceRepo;
    @Autowired private StaffRepository        staffRepo;

    /* ═══════════════════════════════════════════════════════════
       HELPER: lấy Resident đang đăng nhập
    ═══════════════════════════════════════════════════════════ */
    private Residents currentResident() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return residentsRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Resident not found"));
    }

    /* ═══════════════════════════════════════════════════════════
       HOME — thống kê nhanh cho trang chủ
    ═══════════════════════════════════════════════════════════ */
    public Map<String, Object> getHomeSummary() {
        Residents r = currentResident();
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("fullName",   r.getFullName());
        map.put("username",   r.getUsername());

        // Thông tin căn hộ
        if (r.getApartment() != null) {
            Apartment apt = r.getApartment();
            map.put("apartmentId",     apt.getId());
            map.put("apartmentNumber", apt.getNumber());
            map.put("buildingName",    apt.getBuilding() != null ? apt.getBuilding().getName() : "");
            map.put("floor",           apt.getFloor());
            map.put("area",            apt.getArea());
        } else {
            map.put("apartmentId", null);
        }

        // Thông báo chưa đọc
        map.put("unreadNotifications", notifRepo.countUnreadForResident(r));

        // Hóa đơn chưa thanh toán
        long unpaidInvoices = 0;
        if (r.getApartment() != null) {
            unpaidInvoices = invoiceRepo
                .findByApartmentAndStatus(r.getApartment(), Invoice.InvoiceStatus.UNPAID).size();
        }
        map.put("unpaidInvoices", unpaidInvoices);

        // Yêu cầu hỗ trợ đang xử lý
        map.put("pendingRequests",
            srRepo.countByResidentAndStatus(r, ServiceRequest.RequestStatus.PENDING) +
            srRepo.countByResidentAndStatus(r, ServiceRequest.RequestStatus.IN_PROGRESS));

        return map;
    }

    /* ═══════════════════════════════════════════════════════════
       NOTIFICATION — danh sách + đánh dấu đã đọc
    ═══════════════════════════════════════════════════════════ */
    public List<Map<String, Object>> getNotifications() {
        Residents r = currentResident();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Notification n : notifRepo.findForResident(r)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        n.getId());
            m.put("title",     n.getTitle());
            m.put("content",   n.getContent());
            m.put("type",      n.getType().name());
            m.put("isRead",    n.getIsRead());
            m.put("isPersonal", n.getResident() != null); // true = gửi riêng
            m.put("createdAt", n.getCreatedAt().toString());
            m.put("createdBy", n.getCreatedBy() != null ? n.getCreatedBy().getFullName() : "Ban quản lý");
            result.add(m);
        }
        return result;
    }

    @Transactional
    public void markNotificationRead(String notifId) {
        notifRepo.markAsRead(notifId);
    }

    @Transactional
    public void markAllNotificationsRead() {
        notifRepo.markAllAsReadForResident(currentResident());
    }

    /* ═══════════════════════════════════════════════════════════
       INVOICE — danh sách hóa đơn
    ═══════════════════════════════════════════════════════════ */
    public List<Map<String, Object>> getInvoices() {
        Residents r = currentResident();
        List<Map<String, Object>> result = new ArrayList<>();

        if (r.getApartment() == null) return result;

        for (Invoice inv : invoiceRepo.findByApartmentOrderByYearDescMonthDesc(r.getApartment())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          inv.getId());
            m.put("month",       inv.getMonth());
            m.put("year",        inv.getYear());
            m.put("electricKwh", inv.getElectricKwh());
            m.put("waterM3",     inv.getWaterM3());
            m.put("electricAmt", inv.getElectricAmt());
            m.put("waterAmt",    inv.getWaterAmt());
            m.put("serviceAmt",  inv.getServiceAmt());
            m.put("parkingAmt",  inv.getParkingAmt());
            m.put("totalAmt",    inv.getTotalAmt());
            m.put("status",      inv.getStatus().name());
            m.put("dueDate",     inv.getDueDate() != null ? inv.getDueDate().toString() : null);
            m.put("paidAt",      inv.getPaidAt() != null ? inv.getPaidAt().toString() : null);
            result.add(m);
        }
        return result;
    }

    /* ═══════════════════════════════════════════════════════════
       APARTMENT — thông tin căn hộ chi tiết
    ═══════════════════════════════════════════════════════════ */
    public Map<String, Object> getApartmentInfo() {
        Residents r = currentResident();
        Map<String, Object> map = new LinkedHashMap<>();

        if (r.getApartment() == null) {
            map.put("hasApartment", false);
            return map;
        }

        Apartment apt = r.getApartment();
        map.put("hasApartment", true);
        map.put("id",           apt.getId());
        map.put("number",       apt.getNumber());
        map.put("floor",        apt.getFloor());
        map.put("area",         apt.getArea());
        map.put("status",       apt.getStatus().name());
        map.put("rentalStatus", apt.getRentalStatus().name());
        map.put("totalResident",apt.getTotalResident());
        map.put("description",  apt.getDescription());

        if (apt.getBuilding() != null) {
            map.put("buildingName",    apt.getBuilding().getName());
            map.put("buildingAddress", apt.getBuilding().getAddress());
            map.put("totalFloors",     apt.getBuilding().getTotalFloors());
        }

        // Thông tin cư dân hiện tại
        map.put("residentName", r.getFullName());
        map.put("residentType", r.getType().name());

        return map;
    }

    /* ═══════════════════════════════════════════════════════════
       SERVICE REQUEST — danh sách + tạo mới
    ═══════════════════════════════════════════════════════════ */
    public List<Map<String, Object>> getServiceRequests() {
        Residents r = currentResident();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ServiceRequest sr : srRepo.findByResidentOrderByCreatedAtDesc(r)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          sr.getId());
            m.put("title",       sr.getTitle());
            m.put("description", sr.getDescription());
            m.put("category",    sr.getCategory().name());
            m.put("status",      sr.getStatus().name());
            m.put("priority",    sr.getPriority().name());
            m.put("note",        sr.getNote());
            m.put("createdAt",   sr.getCreatedAt().toString());
            m.put("updatedAt",   sr.getUpdatedAt() != null ? sr.getUpdatedAt().toString() : null);
            m.put("assignedTo",  sr.getAssignedTo() != null ? sr.getAssignedTo().getFullName() : null);
            result.add(m);
        }
        return result;
    }

    @Transactional
    public String createServiceRequest(String title, String description, String category, String priority) {
        Residents r = currentResident();

        ServiceRequest sr = new ServiceRequest();
        // ID: SR + timestamp 9 chữ số
        sr.setId("SR" + (System.currentTimeMillis() % 1_000_000_000L));
        sr.setTitle(title);
        sr.setDescription(description);
        sr.setCategory(ServiceRequest.Category.valueOf(category.toUpperCase()));
        sr.setPriority(ServiceRequest.Priority.valueOf(priority.toUpperCase()));
        sr.setStatus(ServiceRequest.RequestStatus.PENDING);
        sr.setResident(r);
        sr.setApartment(r.getApartment()); // có thể null

        srRepo.save(sr);
        return "Yêu cầu hỗ trợ đã được gửi thành công!";
    }
}
