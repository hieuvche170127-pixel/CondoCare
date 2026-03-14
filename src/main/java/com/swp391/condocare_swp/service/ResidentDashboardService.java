package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service tổng hợp cho Resident Dashboard.
 * Mọi method đều tự lấy username từ JWT (SecurityContext)
 * → Cư dân chỉ nhìn thấy dữ liệu của chính mình.
 */
@Service
public class ResidentDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(ResidentDashboardService.class);

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin cư dân"));
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

        // Hóa đơn chưa thanh toán (UNPAID + OVERDUE)
        long unpaidInvoices = 0;
        if (r.getApartment() != null) {
            unpaidInvoices = invoiceRepo.findByApartmentAndStatus(r.getApartment(), Invoice.InvoiceStatus.UNPAID).size()
                    + invoiceRepo.findByApartmentAndStatus(r.getApartment(), Invoice.InvoiceStatus.OVERDUE).size();
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
       INVOICE — danh sách hóa đơn với filter
    ═══════════════════════════════════════════════════════════ */

    /**
     * Lấy danh sách hóa đơn với filter linh hoạt.
     * filterType: "all" | "month" | "quarter" | "year"
     */
    public List<Map<String, Object>> getInvoices(
            String filterType, Integer month, Integer quarter,
            Integer year, String status, String keyword) {

        Residents r = currentResident();
        if (r.getApartment() == null) return new ArrayList<>();

        Apartment apt = r.getApartment();
        List<Invoice> invoices;

        // Tìm kiếm keyword
        if (keyword != null && !keyword.isBlank()) {
            invoices = invoiceRepo.searchByKeyword(apt, keyword.trim());
        }
        // Filter theo tháng
        else if ("month".equalsIgnoreCase(filterType) && month != null && year != null) {
            invoices = invoiceRepo.findByApartmentAndMonthAndYearOrderByYearDescMonthDesc(apt, month, year);
        }
        // Filter theo quý
        else if ("quarter".equalsIgnoreCase(filterType) && quarter != null && year != null) {
            int[] range = quarterToMonthRange(quarter);
            invoices = invoiceRepo.findByApartmentAndQuarterYear(apt, range[0], range[1], year);
        }
        // Filter theo năm
        else if ("year".equalsIgnoreCase(filterType) && year != null) {
            invoices = invoiceRepo.findByApartmentAndYearOrderByYearDescMonthDesc(apt, year);
        }
        // Lọc theo status
        else if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            invoices = invoiceRepo.findByApartmentAndStatus(
                    apt, Invoice.InvoiceStatus.valueOf(status.toUpperCase()));
        }
        // Mặc định: tất cả
        else {
            invoices = invoiceRepo.findByApartmentOrderByYearDescMonthDesc(apt);
        }

        // Áp dụng filter status sau khi đã filter thời gian
        boolean hasStatus = status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL");
        if (hasStatus && !"all".equalsIgnoreCase(filterType)) {
            Invoice.InvoiceStatus s = Invoice.InvoiceStatus.valueOf(status.toUpperCase());
            invoices = invoices.stream().filter(i -> i.getStatus() == s).toList();
        }

        return toInvoiceMapList(invoices);
    }

    /** Giữ lại method cũ không tham số để backward-compatible */
    public List<Map<String, Object>> getInvoices() {
        return getInvoices("all", null, null, null, null, null);
    }

    private List<Map<String, Object>> toInvoiceMapList(List<Invoice> invoices) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Invoice inv : invoices) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          inv.getId());
            m.put("month",       inv.getMonth());
            m.put("year",        inv.getYear());

            // Điện
            BigDecimal electricConsumption = BigDecimal.ZERO;
            BigDecimal electricAmount = BigDecimal.ZERO;
            if (inv.getElectricReading() != null) {
                MeterReading er = inv.getElectricReading();
                electricConsumption = er.getConsumption(); // @Transient — đã trả BigDecimal
                electricAmount = er.getTotalAmount() != null
                        ? BigDecimal.valueOf(er.getTotalAmount()) : BigDecimal.ZERO;
            }
            m.put("electricKwh", electricConsumption);
            m.put("electricAmt", electricAmount);

            // Nước
            BigDecimal waterConsumption = BigDecimal.ZERO;
            BigDecimal waterAmount = BigDecimal.ZERO;
            if (inv.getWaterReading() != null) {
                MeterReading wr = inv.getWaterReading();
                waterConsumption = wr.getConsumption(); // @Transient — đã trả BigDecimal
                waterAmount = wr.getTotalAmount() != null
                        ? BigDecimal.valueOf(wr.getTotalAmount()) : BigDecimal.ZERO;
            }
            m.put("waterM3", waterConsumption);
            m.put("waterAmt", waterAmount);

            // Phí dịch vụ & đỗ xe
            BigDecimal serviceAmt = (inv.getServiceFee() != null && inv.getServiceFee().getAmount() != null)
                    ? inv.getServiceFee().getAmount() : BigDecimal.ZERO;
            BigDecimal parkingAmt = (inv.getParkingFee() != null && inv.getParkingFee().getAmount() != null)
                    ? inv.getParkingFee().getAmount() : BigDecimal.ZERO;

            m.put("serviceAmt", serviceAmt);
            m.put("parkingAmt", parkingAmt);

            m.put("electricAmount", inv.getElectricAmount() != null ? inv.getElectricAmount() : electricAmount);
            m.put("waterAmount",    inv.getWaterAmount()    != null ? inv.getWaterAmount()    : waterAmount);
            m.put("serviceAmount",  inv.getServiceAmount()  != null ? inv.getServiceAmount()  : serviceAmt);
            m.put("parkingAmount",  inv.getParkingAmount()  != null ? inv.getParkingAmount()  : parkingAmt);
            m.put("totalAmt",       inv.getTotalAmount()    != null ? inv.getTotalAmount()    : BigDecimal.ZERO);

            m.put("status",    inv.getStatus().name());
            m.put("dueDate",   inv.getDueDate()  != null ? inv.getDueDate().toString()  : null);
            m.put("paidAt",    inv.getPaidAt()   != null ? inv.getPaidAt().toString()   : null);
            m.put("createdBy", inv.getCreatedBy() != null ? inv.getCreatedBy().getFullName() : "Hệ thống");

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
       SERVICE REQUEST — danh sách + filter + tạo mới
    ═══════════════════════════════════════════════════════════ */

    /**
     * Lấy danh sách yêu cầu hỗ trợ với filter linh hoạt.
     *
     * @param filterType "all" | "month" | "quarter" | "year"
     * @param month      số tháng (1-12), dùng khi filterType=month
     * @param quarter    số quý (1-4), dùng khi filterType=quarter
     * @param year       năm, dùng khi filterType=month/quarter/year
     * @param status     null/"ALL" hoặc tên enum: PENDING/IN_PROGRESS/DONE/REJECTED
     * @param keyword    chuỗi tìm kiếm trong title + description
     */
    public List<Map<String, Object>> getServiceRequests(
            String filterType, Integer month, Integer quarter,
            Integer year, String status, String keyword) {

        Residents r = currentResident();
        List<ServiceRequest> list;

        // Tìm kiếm keyword ưu tiên cao nhất
        if (keyword != null && !keyword.isBlank()) {
            list = srRepo.searchByKeyword(r, keyword.trim());
        }
        // Filter theo tháng
        else if ("month".equalsIgnoreCase(filterType) && month != null && year != null) {
            list = srRepo.findByResidentAndMonthYear(r, month, year);
        }
        // Filter theo quý
        else if ("quarter".equalsIgnoreCase(filterType) && quarter != null && year != null) {
            int[] range = quarterToMonthRange(quarter);
            list = srRepo.findByResidentAndQuarterYear(r, range[0], range[1], year);
        }
        // Filter theo năm
        else if ("year".equalsIgnoreCase(filterType) && year != null) {
            list = srRepo.findByResidentAndYear(r, year);
        }
        // Lọc theo status đơn thuần
        else if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            list = srRepo.findByResidentAndStatusOrderByCreatedAtDesc(
                    r, ServiceRequest.RequestStatus.valueOf(status.toUpperCase()));
        }
        // Mặc định: tất cả
        else {
            list = srRepo.findByResidentOrderByCreatedAtDesc(r);
        }

        // Áp dụng filter status sau khi đã filter thời gian (nếu có)
        boolean hasStatus = status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL");
        if (hasStatus && !"all".equalsIgnoreCase(filterType) &&
                keyword == null || (keyword != null && keyword.isBlank())) {
            ServiceRequest.RequestStatus s = ServiceRequest.RequestStatus.valueOf(status.toUpperCase());
            list = list.stream().filter(sr -> sr.getStatus() == s).toList();
        }

        return toMapList(list);
    }

    /** Giữ lại method cũ không tham số để backward-compatible */
    public List<Map<String, Object>> getServiceRequests() {
        return getServiceRequests("all", null, null, null, null, null);
    }

    private List<Map<String, Object>> toMapList(List<ServiceRequest> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceRequest sr : list) {
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

    private int[] quarterToMonthRange(int quarter) {
        return switch (quarter) {
            case 1  -> new int[]{1, 3};
            case 2  -> new int[]{4, 6};
            case 3  -> new int[]{7, 9};
            case 4  -> new int[]{10, 12};
            default -> new int[]{1, 12};
        };
    }

    @Transactional
    public String createServiceRequest(String title, String description,
                                       String category, String priority) {
        logger.info("=== createServiceRequest START === title: [{}], category: [{}], priority: [{}]",
                title, category, priority);

        // 1. Validate input trước khi làm gì
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Tiêu đề không được để trống");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Mô tả không được để trống");

        // 2. Lấy resident từ SecurityContext
        Residents r = currentResident();
        logger.info("Current resident ID: [{}], username: [{}]", r.getId(), r.getUsername());

        // 3. Build entity — KHÔNG bọc try-catch để @Transactional rollback đúng khi lỗi
        ServiceRequest sr = new ServiceRequest();

        // ID: "SR" + 13 ký tự = 15 ký tự <= VARCHAR(15) trong DB
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String newId = "SR" + uuid.substring(0, 13);
        sr.setId(newId);
        logger.info("Generated ID: [{}] (length={})", newId, newId.length());

        sr.setTitle(title.trim());
        sr.setDescription(description.trim());
        sr.setStatus(ServiceRequest.RequestStatus.PENDING);

        // Parse enum — nếu sai giá trị sẽ ném IllegalArgumentException rõ ràng
        try {
            sr.setCategory(ServiceRequest.Category.valueOf(category.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Category không hợp lệ: " + category +
                    ". Giá trị hợp lệ: ELECTRIC, WATER, INTERNET, HVAC, STRUCTURE, OTHER");
        }
        try {
            sr.setPriority(ServiceRequest.Priority.valueOf(priority.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Priority không hợp lệ: " + priority +
                    ". Giá trị hợp lệ: LOW, MEDIUM, HIGH");
        }

        sr.setResident(r);
        sr.setApartment(r.getApartment()); // null nếu chưa có apartment → OK vì apartment_id nullable

        logger.info("Saving ServiceRequest: id=[{}], residentId=[{}], apartmentId=[{}]",
                sr.getId(),
                sr.getResident() != null ? sr.getResident().getId() : "NULL",
                sr.getApartment() != null ? sr.getApartment().getId() : "NULL");

        // 4. Save — nếu lỗi DB sẽ ném exception, @Transactional tự rollback
        ServiceRequest saved = srRepo.save(sr);
        logger.info("=== createServiceRequest SUCCESS === saved ID: [{}]", saved.getId());

        return "Yêu cầu hỗ trợ đã được gửi thành công!";
    }

    @Transactional
    public String markInvoiceAsPaid(String invoiceId) {
        Residents r = currentResident();

        Invoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        // Kiểm tra quyền
        if (r.getApartment() == null || !invoice.getApartment().getId().equals(r.getApartment().getId())) {
            throw new RuntimeException("Bạn không có quyền thanh toán hóa đơn này");
        }

        // Kiểm tra trạng thái
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new RuntimeException("Hóa đơn đã được thanh toán");
        }

        // Cập nhật
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepo.save(invoice);

        logger.info("Invoice {} marked as paid by resident {}", invoiceId, r.getId());

        return "Thanh toán thành công!";
    }
}