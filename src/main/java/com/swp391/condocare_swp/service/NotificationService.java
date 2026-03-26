package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * NotificationService — Luồng quản lý thông báo phía Staff.
 *
 * 3 loại gửi:
 *   1. Broadcast toàn tòa   → resident_id = NULL, building_id = set
 *   2. Gửi theo căn hộ      → apartment_id = set, resident_id = NULL
 *   3. Gửi cá nhân          → resident_id = set
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired private NotificationRepository notifRepo;
    @Autowired private ResidentsRepository    residentsRepo;
    @Autowired private ApartmentRepository    apartmentRepo;
    @Autowired private BuildingRepository     buildingRepo;
    @Autowired private StaffRepository        staffRepo;

    // ─── THỐNG KÊ ─────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",     notifRepo.count());
        m.put("broadcast", notifRepo.countByResidentIsNull());
        m.put("personal",  notifRepo.countByResidentIsNotNull());
        m.put("unread",    notifRepo.countByIsRead(false));
        return m;
    }

    // ─── DANH SÁCH (Staff xem) ────────────────────────────────────────────────

    public List<Map<String, Object>> getAllNotifications(
            String type, String buildingId, String residentId) {

        List<Notification> list;

        if (residentId != null && !residentId.isBlank()) {
            Residents r = residentsRepo.findById(residentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + residentId));
            list = notifRepo.findByResidentOrderByCreatedAtDesc(r);
        } else if (buildingId != null && !buildingId.isBlank()) {
            list = notifRepo.findByBuildingIdOrderByCreatedAtDesc(buildingId);
        } else {
            list = notifRepo.findAllByOrderByCreatedAtDesc();
        }

        if (type != null && !type.isBlank() && !type.equals("ALL")) {
            Notification.NotificationType t = Notification.NotificationType.valueOf(type);
            list = list.stream().filter(n -> n.getType() == t).toList();
        }

        return list.stream().map(this::mapToResponse).toList();
    }

    // ─── GỬI THÔNG BÁO ───────────────────────────────────────────────────────

    /**
     * Gửi broadcast toàn tòa nhà.
     * Body: { title, content, type, buildingId }
     */
    @Transactional
    public String sendBroadcast(Map<String, String> body) {
        String buildingId = body.get("buildingId");
        if (buildingId == null || buildingId.isBlank())
            throw new RuntimeException("buildingId không được để trống.");

        Building building = buildingRepo.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà: " + buildingId));

        Notification n = buildNotification(body, getCurrentStaff());
        n.setBuilding(building);
        n.setResident(null);
        n.setApartment(null);
        notifRepo.save(n);

        logger.info("Broadcast notification {} sent to building {}", n.getId(), buildingId);
        return "Đã gửi thông báo broadcast cho toàn bộ tòa nhà.";
    }

    /**
     * Gửi thông báo theo căn hộ (tất cả cư dân trong căn đó).
     * Body: { title, content, type, apartmentId }
     */
    @Transactional
    public String sendToApartment(Map<String, String> body) {
        String apartmentId = body.get("apartmentId");
        if (apartmentId == null || apartmentId.isBlank())
            throw new RuntimeException("apartmentId không được để trống.");

        Apartment apt = apartmentRepo.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + apartmentId));

        Staff staff = getCurrentStaff();
        Notification n = buildNotification(body, staff);
        n.setApartment(apt);
        n.setBuilding(apt.getBuilding());
        n.setResident(null);
        notifRepo.save(n);

        logger.info("Notification {} sent to apartment {}", n.getId(), apartmentId);
        return "Đã gửi thông báo đến căn hộ " + apt.getNumber() + ".";
    }

    /**
     * Gửi thông báo cá nhân cho 1 cư dân.
     * Body: { title, content, type, residentId }
     */
    @Transactional
    public String sendToResident(Map<String, String> body) {
        String residentId = body.get("residentId");
        if (residentId == null || residentId.isBlank())
            throw new RuntimeException("residentId không được để trống.");

        Residents resident = residentsRepo.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + residentId));

        Staff staff = getCurrentStaff();
        Notification n = buildNotification(body, staff);
        n.setResident(resident);
        n.setApartment(resident.getApartment());
        n.setBuilding(resident.getApartment() != null ? resident.getApartment().getBuilding() : null);
        notifRepo.save(n);

        logger.info("Notification {} sent to resident {}", n.getId(), residentId);
        return "Đã gửi thông báo đến cư dân " + resident.getFullName() + ".";
    }

    /**
     * Gửi thông báo nhắc thanh toán hóa đơn quá hạn cho 1 cư dân.
     * Thường được gọi tự động từ InvoiceManagementService.
     */
    @Transactional
    public void sendPaymentReminder(Residents resident, String invoiceId,
                                    String amount, Staff staff) {
        Notification n = new Notification();
        n.setId(generateId());
        n.setTitle("Nhắc nhở: Hóa đơn chưa thanh toán");
        n.setContent("Hóa đơn " + invoiceId + " với số tiền " + amount +
                " đã quá hạn. Vui lòng thanh toán sớm để tránh bị tạm ngừng dịch vụ.");
        n.setType(Notification.NotificationType.PAYMENT);
        n.setResident(resident);
        n.setApartment(resident.getApartment());
        n.setBuilding(resident.getApartment() != null ? resident.getApartment().getBuilding() : null);
        n.setCreatedBy(staff);
        n.setIsRead(false);
        notifRepo.save(n);
    }

    // ─── XÓA THÔNG BÁO ───────────────────────────────────────────────────────

    @Transactional
    public String deleteNotification(String id) {
        Notification n = notifRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo: " + id));
        notifRepo.delete(n);
        logger.info("Notification {} deleted", id);
        return "Đã xóa thông báo thành công.";
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private Notification buildNotification(Map<String, String> body, Staff staff) {
        String title   = body.get("title");
        String content = body.get("content");
        String typeStr = body.getOrDefault("type", "INFO");

        if (title == null || title.isBlank())
            throw new RuntimeException("Tiêu đề thông báo không được để trống.");
        if (content == null || content.isBlank())
            throw new RuntimeException("Nội dung thông báo không được để trống.");

        Notification.NotificationType type;
        try {
            type = Notification.NotificationType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = Notification.NotificationType.INFO;
        }

        Notification n = new Notification();
        n.setId(generateId());
        n.setTitle(title.trim());
        n.setContent(content.trim());
        n.setType(type);
        n.setCreatedBy(staff);
        n.setIsRead(false);
        return n;
    }

    private Map<String, Object> mapToResponse(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         n.getId());
        m.put("title",      n.getTitle());
        m.put("content",    n.getContent());
        m.put("type",       n.getType().name());
        m.put("isRead",     n.getIsRead());
        m.put("createdAt",  n.getCreatedAt().toString());
        m.put("createdBy",  n.getCreatedBy() != null ? n.getCreatedBy().getFullName() : "Hệ thống");

        // Phân biệt loại gửi
        if (n.getResident() != null) {
            m.put("sendType",     "PERSONAL");
            m.put("residentId",   n.getResident().getId());
            m.put("residentName", n.getResident().getFullName());
        } else if (n.getApartment() != null) {
            m.put("sendType",        "APARTMENT");
            m.put("apartmentId",     n.getApartment().getId());
            m.put("apartmentNumber", n.getApartment().getNumber());
        } else {
            m.put("sendType",     "BROADCAST");
        }

        if (n.getBuilding() != null) {
            m.put("buildingId",   n.getBuilding().getId());
            m.put("buildingName", n.getBuilding().getName());
        }

        return m;
    }

    private Staff getCurrentStaff() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return staffRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Staff đang đăng nhập."));
    }

    private String generateId() {
        return "NTF" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}