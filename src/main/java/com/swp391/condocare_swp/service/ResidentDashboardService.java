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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service tổng hợp cho Resident Dashboard — Mô hình B.
 * Hóa đơn chỉ gồm phí dịch vụ + phí gửi xe (không có điện/nước).
 * Mọi method tự lấy username từ JWT → cư dân chỉ thấy dữ liệu của mình.
 */
@Service
public class ResidentDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(ResidentDashboardService.class);

    @Autowired private ResidentsRepository        residentsRepo;
    @Autowired private NotificationRepository     notifRepo;
    @Autowired private ServiceRequestRepository   srRepo;
    @Autowired private InvoiceRepository          invoiceRepo;
    @Autowired private InvoiceFeeDetailRepository detailRepo;
    @Autowired private VehicleRepository          vehicleRepo;
    @Autowired private AccessCardRepository       accessCardRepo;

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private Residents currentResident() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return residentsRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin cư dân."));
    }

    // ─── HOME ─────────────────────────────────────────────────────────────────

    public Map<String, Object> getHomeSummary() {
        Residents r = currentResident();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullName", r.getFullName());
        map.put("username", r.getUsername());
        map.put("status",   r.getStatus().name());
        map.put("type",     r.getType().name());

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

        map.put("unreadNotifications", notifRepo.countUnreadForResident(r));

        long unpaid = 0;
        if (r.getApartment() != null) {
            unpaid = invoiceRepo.findByApartmentAndStatus(r.getApartment(), Invoice.InvoiceStatus.UNPAID).size()
                    + invoiceRepo.findByApartmentAndStatus(r.getApartment(), Invoice.InvoiceStatus.OVERDUE).size();
        }
        map.put("unpaidInvoices", unpaid);

        map.put("pendingRequests",
                srRepo.countByResidentAndStatus(r, ServiceRequest.RequestStatus.PENDING)
                        + srRepo.countByResidentAndStatus(r, ServiceRequest.RequestStatus.IN_PROGRESS));

        // Số xe đang APPROVED + ACTIVE
        long activeVehicles = vehicleRepo.findByResidentId(r.getId()).stream()
                .filter(v -> v.getPendingStatus() == Vehicle.PendingStatus.APPROVED
                        && v.getStatus() == Vehicle.VehicleStatus.ACTIVE)
                .count();
        map.put("totalVehicles", activeVehicles);

        // Thẻ ra vào
        long activeCards = accessCardRepo.countByResidentIdAndStatus(
                r.getId(), AccessCard.CardStatus.ACTIVE);
        map.put("hasAccessCard", activeCards > 0);

        return map;
    }

    // ─── NOTIFICATIONS ────────────────────────────────────────────────────────

    public List<Map<String, Object>> getNotifications() {
        Residents r = currentResident();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : notifRepo.findForResident(r)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",         n.getId());
            m.put("title",      n.getTitle());
            m.put("content",    n.getContent());
            m.put("type",       n.getType().name());
            m.put("isRead",     n.getIsRead());
            m.put("isPersonal", n.getResident() != null);
            m.put("createdAt",  n.getCreatedAt().toString());
            m.put("createdBy",  n.getCreatedBy() != null ? n.getCreatedBy().getFullName() : "Ban quản lý");
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

    // ─── INVOICES ─────────────────────────────────────────────────────────────

    /**
     * Danh sách hóa đơn với filter linh hoạt.
     * filterType: "all" | "month" | "quarter" | "year"
     * Mỗi hóa đơn có feeDetails (phí dịch vụ + phí gửi xe), KHÔNG có điện/nước.
     */
    public List<Map<String, Object>> getInvoices(
            String filterType, Integer month, Integer quarter,
            Integer year, String status, String keyword) {

        Residents r = currentResident();
        if (r.getApartment() == null) return new ArrayList<>();

        Apartment apt = r.getApartment();
        List<Invoice> invoices;

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            invoices = invoiceRepo.findByApartmentOrderByYearDescMonthDesc(apt).stream()
                    .filter(i -> i.getId().toLowerCase().contains(kw)
                            || i.getStatus().name().toLowerCase().contains(kw))
                    .toList();
        } else if ("month".equalsIgnoreCase(filterType) && month != null && year != null) {
            invoices = invoiceRepo.findByApartmentAndMonthAndYear(apt, month, year)
                    .map(List::of).orElse(List.of());
        } else if ("quarter".equalsIgnoreCase(filterType) && quarter != null && year != null) {
            int[] range = quarterToMonthRange(quarter);
            invoices = invoiceRepo.findByApartmentOrderByYearDescMonthDesc(apt).stream()
                    .filter(i -> year.equals(i.getYear())
                            && i.getMonth() >= range[0] && i.getMonth() <= range[1])
                    .toList();
        } else if ("year".equalsIgnoreCase(filterType) && year != null) {
            invoices = invoiceRepo.findByApartmentOrderByYearDescMonthDesc(apt).stream()
                    .filter(i -> year.equals(i.getYear())).toList();
        } else if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            invoices = invoiceRepo.findByApartmentAndStatus(
                    apt, Invoice.InvoiceStatus.valueOf(status.toUpperCase()));
        } else {
            invoices = invoiceRepo.findByApartmentOrderByYearDescMonthDesc(apt);
        }

        // Filter status sau nếu đã filter theo thời gian
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")
                && !"all".equalsIgnoreCase(filterType)) {
            Invoice.InvoiceStatus s = Invoice.InvoiceStatus.valueOf(status.toUpperCase());
            invoices = invoices.stream().filter(i -> i.getStatus() == s).toList();
        }

        return invoices.stream().map(this::mapInvoice).toList();
    }

    public List<Map<String, Object>> getInvoices() {
        return getInvoices("all", null, null, null, null, null);
    }

    private Map<String, Object> mapInvoice(Invoice inv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",        inv.getId());
        m.put("month",     inv.getMonth());
        m.put("year",      inv.getYear());
        m.put("totalAmt",  inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
        m.put("status",    inv.getStatus().name());
        m.put("issuedAt",  inv.getIssuedAt()  != null ? inv.getIssuedAt().toString()  : null);
        m.put("dueDate",   inv.getDueDate()   != null ? inv.getDueDate().toString()   : null);
        m.put("paidAt",    inv.getPaidAt()    != null ? inv.getPaidAt().toString()    : null);
        m.put("createdBy", inv.getCreatedBy() != null ? inv.getCreatedBy().getFullName() : "Hệ thống");

        // Chi tiết từng dòng phí (thay cho electricAmount/waterAmount cũ)
        List<InvoiceFeeDetail> details = detailRepo.findByInvoiceId(inv.getId());
        List<Map<String, Object>> feeLines = details.stream().map(d -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("feeName",    d.getFeeName());
            line.put("feeType",    d.getFeeType().name());
            line.put("unitAmount", d.getUnitAmount());
            line.put("quantity",   d.getQuantity());
            line.put("amount",     d.getAmount());
            return line;
        }).toList();
        m.put("feeDetails", feeLines);

        // Tổng riêng SERVICE và PARKING để UI hiển thị
        BigDecimal serviceTotal = details.stream()
                .filter(d -> d.getFeeType() == FeeTemplate.FeeType.SERVICE)
                .map(InvoiceFeeDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal parkingTotal = details.stream()
                .filter(d -> d.getFeeType() == FeeTemplate.FeeType.PARKING)
                .map(InvoiceFeeDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("serviceTotal", serviceTotal);
        m.put("parkingTotal", parkingTotal);

        return m;
    }

    // ─── APARTMENT ────────────────────────────────────────────────────────────

    public Map<String, Object> getApartmentInfo() {
        Residents r = currentResident();
        Map<String, Object> map = new LinkedHashMap<>();

        if (r.getApartment() == null) {
            map.put("hasApartment", false);
            return map;
        }

        Apartment apt = r.getApartment();
        map.put("hasApartment",  true);
        map.put("id",            apt.getId());
        map.put("number",        apt.getNumber());
        map.put("floor",         apt.getFloor());
        map.put("area",          apt.getArea());
        map.put("status",        apt.getStatus().name());
        map.put("rentalStatus",  apt.getRentalStatus().name());
        map.put("totalResident", apt.getTotalResident());
        map.put("description",   apt.getDescription());

        if (apt.getBuilding() != null) {
            map.put("buildingName",    apt.getBuilding().getName());
            map.put("buildingAddress", apt.getBuilding().getAddress());
            map.put("totalFloors",     apt.getBuilding().getTotalFloors());
        }

        map.put("residentName", r.getFullName());
        map.put("residentType", r.getType().name());

        // Thẻ ra vào của cư dân
        List<Map<String, Object>> cards = accessCardRepo.findByResidentId(r.getId())
                .stream().map(c -> {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("cardNumber", c.getCardNumber());
                    cm.put("status",     c.getStatus().name());
                    cm.put("issuedAt",   c.getIssuedAt()  != null ? c.getIssuedAt().toString()  : null);
                    cm.put("expiredAt",  c.getExpiredAt() != null ? c.getExpiredAt().toString() : null);
                    return cm;
                }).toList();
        map.put("accessCards", cards);

        // Xe của cư dân trong căn hộ này
        List<Map<String, Object>> vehicles = vehicleRepo.findByResidentId(r.getId())
                .stream().map(v -> {
                    Map<String, Object> vm = new LinkedHashMap<>();
                    vm.put("id",            v.getId());
                    vm.put("type",          v.getType().name());
                    vm.put("licensePlate",  v.getLicensePlate());
                    vm.put("brand",         v.getBrand());
                    vm.put("color",         v.getColor());
                    vm.put("pendingStatus", v.getPendingStatus().name());
                    vm.put("status",        v.getStatus().name());
                    vm.put("expiredAt",     v.getExpiredAt() != null ? v.getExpiredAt().toString() : null);
                    return vm;
                }).toList();
        map.put("vehicles", vehicles);

        return map;
    }

    // ─── SERVICE REQUESTS ─────────────────────────────────────────────────────

    public List<Map<String, Object>> getServiceRequests(
            String filterType, Integer month, Integer quarter,
            Integer year, String status, String keyword) {

        Residents r = currentResident();
        List<ServiceRequest> list;

        if (keyword != null && !keyword.isBlank()) {
            list = srRepo.searchByKeyword(r, keyword.trim());
        } else if ("month".equalsIgnoreCase(filterType) && month != null && year != null) {
            list = srRepo.findByResidentAndMonthYear(r, month, year);
        } else if ("quarter".equalsIgnoreCase(filterType) && quarter != null && year != null) {
            int[] range = quarterToMonthRange(quarter);
            list = srRepo.findByResidentAndQuarterYear(r, range[0], range[1], year);
        } else if ("year".equalsIgnoreCase(filterType) && year != null) {
            list = srRepo.findByResidentAndYear(r, year);
        } else if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            list = srRepo.findByResidentAndStatusOrderByCreatedAtDesc(
                    r, ServiceRequest.RequestStatus.valueOf(status.toUpperCase()));
        } else {
            list = srRepo.findByResidentOrderByCreatedAtDesc(r);
        }

        // Filter status sau nếu đã filter theo thời gian
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")
                && !"all".equalsIgnoreCase(filterType)
                && (keyword == null || keyword.isBlank())) {
            ServiceRequest.RequestStatus s = ServiceRequest.RequestStatus.valueOf(status.toUpperCase());
            list = list.stream().filter(sr -> sr.getStatus() == s).toList();
        }

        return list.stream().map(this::mapServiceRequest).toList();
    }

    public List<Map<String, Object>> getServiceRequests() {
        return getServiceRequests("all", null, null, null, null, null);
    }

    public Map<String, Object> getServiceRequestDetail(String requestId) {
        Residents r = currentResident();
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu."));
        if (!sr.getResident().getId().equals(r.getId()))
            throw new RuntimeException("Bạn không có quyền xem yêu cầu này.");

        Map<String, Object> m = mapServiceRequest(sr);
        m.put("completionImage", sr.getCompletionImage()); // base64 đầy đủ chỉ trong detail
        return m;
    }

    @Transactional
    public String createServiceRequest(String title, String description,
                                       String category, String priority) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Tiêu đề không được để trống.");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Mô tả không được để trống.");

        Residents r = currentResident();

        ServiceRequest sr = new ServiceRequest();
        // ID: "SR" + 13 ký tự → tổng 15 ký tự khớp VARCHAR(15)
        String uid = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        sr.setId("SR" + uid.substring(0, 13));
        sr.setTitle(title.trim());
        sr.setDescription(description.trim());
        sr.setStatus(ServiceRequest.RequestStatus.PENDING);
        sr.setResident(r);
        sr.setApartment(r.getApartment());

        try {
            sr.setCategory(ServiceRequest.Category.valueOf(category.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Danh mục không hợp lệ: " + category);
        }
        try {
            sr.setPriority(ServiceRequest.Priority.valueOf(priority.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Mức ưu tiên không hợp lệ: " + priority);
        }

        srRepo.save(sr);
        logger.info("Service request created: {} by resident {}", sr.getId(), r.getId());
        return "Yêu cầu hỗ trợ đã được gửi thành công!";
    }

    @Transactional
    public String confirmServiceRequest(String requestId) {
        Residents r = currentResident();
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu."));

        if (!sr.getResident().getId().equals(r.getId()))
            throw new RuntimeException("Bạn không có quyền xác nhận yêu cầu này.");
        if (sr.getStatus() != ServiceRequest.RequestStatus.DONE)
            throw new RuntimeException("Chỉ có thể xác nhận yêu cầu đã hoàn thành (DONE).");
        if (Boolean.TRUE.equals(sr.getResidentConfirmed()))
            throw new RuntimeException("Bạn đã xác nhận yêu cầu này rồi.");

        sr.setResidentConfirmed(true);
        sr.setConfirmedAt(LocalDateTime.now());
        srRepo.save(sr);
        return "Xác nhận thành công! Cảm ơn bạn đã phản hồi.";
    }

    @Transactional
    public String markInvoiceAsPaid(String invoiceId) {
        Residents r = currentResident();
        Invoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn."));

        if (r.getApartment() == null
                || !invoice.getApartment().getId().equals(r.getApartment().getId()))
            throw new RuntimeException("Bạn không có quyền thanh toán hóa đơn này.");
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
            throw new RuntimeException("Hóa đơn đã được thanh toán rồi.");

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepo.save(invoice);
        return "Thanh toán thành công!";
    }

    // ─── VEHICLE REGISTRATION ─────────────────────────────────────────────────

    /**
     * Cư dân đăng ký gửi xe mới.
     * Vehicle được tạo với pending_status = PENDING, chờ BQL duyệt.
     *
     * @param body Map chứa: type, licensePlate?, brand?, model?, color?, durationType
     */
    @Transactional
    public String registerVehicle(Map<String, String> body) {
        Residents r = currentResident();

        if (r.getApartment() == null)
            throw new IllegalArgumentException("Bạn chưa được gán căn hộ. Không thể đăng ký gửi xe.");

        // Validate type
        String typeStr = body.get("type");
        if (typeStr == null || typeStr.isBlank())
            throw new IllegalArgumentException("Vui lòng chọn loại xe.");
        Vehicle.VehicleType type;
        try {
            type = Vehicle.VehicleType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại xe không hợp lệ: " + typeStr);
        }

        // Validate durationType
        String durationStr = body.getOrDefault("durationType", "MONTHLY");
        Vehicle.DurationType durationType;
        try {
            durationType = Vehicle.DurationType.valueOf(durationStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Thời hạn đăng ký không hợp lệ: " + durationStr);
        }

        // Kiểm tra biển số trùng (nếu có)
        String licensePlate = body.get("licensePlate");
        if (licensePlate != null && !licensePlate.isBlank()) {
            licensePlate = licensePlate.trim().toUpperCase();
            if (vehicleRepo.existsByLicensePlate(licensePlate))
                throw new IllegalArgumentException(
                        "Biển số " + licensePlate + " đã được đăng ký trong hệ thống.");
        } else {
            licensePlate = null; // xe đạp không cần biển
        }

        // Sinh Vehicle ID
        String vehicleId = generateVehicleId();

        Vehicle v = new Vehicle();
        v.setId(vehicleId);
        v.setType(type);
        v.setLicensePlate(licensePlate);
        v.setBrand(body.get("brand") != null ? body.get("brand").trim() : null);
        v.setModel(body.get("model") != null ? body.get("model").trim() : null);
        v.setColor(body.get("color") != null ? body.get("color").trim() : null);
        v.setResident(r.getId());
        v.setApartment(r.getApartment().getId());
        v.setDurationType(durationType);
        v.setPendingStatus(Vehicle.PendingStatus.PENDING);
        v.setStatus(Vehicle.VehicleStatus.ACTIVE);

        vehicleRepo.save(v);
        logger.info("Vehicle registration submitted — id={}, type={}, plate={}, residentId={}",
                vehicleId, type, licensePlate, r.getId());

        return "Đăng ký gửi xe thành công! Vui lòng chờ Ban quản lý xem xét và phê duyệt.";
    }

    private synchronized String generateVehicleId() {
        for (int i = 1; i <= 99999; i++) {
            String id = "VH" + String.format("%06d", i);
            if (!vehicleRepo.existsById(id)) return id;
        }
        return "VH" + System.currentTimeMillis() % 10000000L;
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private Map<String, Object> mapServiceRequest(ServiceRequest sr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               sr.getId());
        m.put("title",            sr.getTitle());
        m.put("description",      sr.getDescription());
        m.put("category",         sr.getCategory().name());
        m.put("status",           sr.getStatus().name());
        m.put("priority",         sr.getPriority().name());
        m.put("note",             sr.getNote());
        m.put("rejectReason",     sr.getRejectReason());
        m.put("residentConfirmed", sr.getResidentConfirmed());
        m.put("confirmedAt",      sr.getConfirmedAt() != null ? sr.getConfirmedAt().toString() : null);
        m.put("hasImage",         sr.getCompletionImage() != null && !sr.getCompletionImage().isBlank());
        m.put("createdAt",        sr.getCreatedAt().toString());
        m.put("updatedAt",        sr.getUpdatedAt() != null ? sr.getUpdatedAt().toString() : null);
        m.put("assignedTo",       sr.getAssignedTo() != null ? sr.getAssignedTo().getFullName() : null);
        return m;
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
}