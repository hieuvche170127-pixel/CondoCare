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
 * NotificationService — Luồng quản lý thông báo.
 *
 * 3 loại gửi từ Staff:
 *   1. Broadcast toàn tòa   → resident_id = NULL, building_id = set
 *   2. Gửi theo căn hộ      → apartment_id = set, resident_id = NULL
 *   3. Gửi cá nhân          → resident_id = set
 *
 * Thông báo hệ thống tự động (gọi từ các service khác):
 *   - sendAccountApprovedNotification()  → khi manager duyệt tài khoản
 *   - sendAccessCardIssuedNotification() → khi manager cấp thẻ ra vào
 *   - sendRequestStatusNotification()    → khi staff cập nhật trạng thái yêu cầu
 *   - sendPaymentReminder()              → nhắc hóa đơn quá hạn
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

    // ─── GỬI THÔNG BÁO (Staff thao tác thủ công) ─────────────────────────────

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
     * Gửi thông báo theo căn hộ.
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

    // ─── THÔNG BÁO HỆ THỐNG TỰ ĐỘNG ──────────────────────────────────────────

    /**
     * Gửi thông báo khi tài khoản cư dân được duyệt (PENDING → ACTIVE).
     * Gọi từ ResidentManagementService.approveResident().
     */
    @Transactional
    public void sendAccountApprovedNotification(Residents resident, Staff issuedBy) {
        Notification n = new Notification();
        n.setId(generateId());
        n.setTitle("Tài khoản của bạn đã được xác minh");
        n.setContent("Chào mừng " + resident.getFullName() + " đến với CondoCare! " +
                "Tài khoản đã được Ban quản lý kích hoạt. " +
                "Bạn có thể sử dụng đầy đủ các tính năng: nhận thông báo, " +
                "xem hóa đơn, đăng ký xe và gửi yêu cầu hỗ trợ.");
        n.setType(Notification.NotificationType.INFO);
        n.setResident(resident);
        n.setApartment(resident.getApartment());
        n.setBuilding(resident.getApartment() != null ? resident.getApartment().getBuilding() : null);
        n.setCreatedBy(issuedBy);
        n.setIsRead(false);
        notifRepo.save(n);
        logger.info("Account approved notification sent to resident {}", resident.getId());
    }

    /**
     * Gửi thông báo khi thẻ ra vào được cấp (sau duyệt tài khoản hoặc cấp lại).
     * Gọi từ ResidentManagementService sau khi issueAccessCard().
     */
    @Transactional
    public void sendAccessCardIssuedNotification(Residents resident, String cardNumber, Staff issuedBy) {
        Notification n = new Notification();
        n.setId(generateId());
        n.setTitle("Thẻ ra vào đã được cấp");
        n.setContent("Thẻ ra vào chung cư của bạn đã được cấp với số thẻ: " + cardNumber + ". " +
                "Vui lòng đến văn phòng Ban quản lý để nhận thẻ vật lý. " +
                "Thẻ có hiệu lực trong 2 năm kể từ ngày cấp.");
        n.setType(Notification.NotificationType.INFO);
        n.setResident(resident);
        n.setApartment(resident.getApartment());
        n.setBuilding(resident.getApartment() != null ? resident.getApartment().getBuilding() : null);
        n.setCreatedBy(issuedBy);
        n.setIsRead(false);
        notifRepo.save(n);
        logger.info("Access card issued notification sent to resident {} — card {}", resident.getId(), cardNumber);
    }

    /**
     * Gửi thông báo khi trạng thái yêu cầu hỗ trợ thay đổi.
     * Gọi từ StaffServiceRequestService khi assign, done, hoặc reject.
     *
     * @param resident    Cư dân cần nhận thông báo
     * @param requestId   ID yêu cầu
     * @param requestTitle Tiêu đề yêu cầu
     * @param newStatus   Trạng thái mới (IN_PROGRESS, DONE, REJECTED)
     * @param note        Ghi chú thêm (lý do từ chối, ghi chú hoàn thành, ...)
     * @param staff       Staff thực hiện thay đổi
     */
    @Transactional
    public void sendRequestStatusNotification(Residents resident, String requestId,
                                              String requestTitle, String newStatus,
                                              String note, Staff staff) {
        String title;
        String content;
        Notification.NotificationType type = Notification.NotificationType.INFO;

        switch (newStatus.toUpperCase()) {
            case "IN_PROGRESS" -> {
                title   = "Yêu cầu hỗ trợ đang được xử lý";
                content = "Yêu cầu \"" + requestTitle + "\" (#" + requestId + ") " +
                        "đã được tiếp nhận và đang trong quá trình xử lý.";
                if (note != null && !note.isBlank())
                    content += " Ghi chú: " + note;
            }
            case "DONE" -> {
                title   = "Yêu cầu hỗ trợ đã hoàn thành";
                content = "Yêu cầu \"" + requestTitle + "\" (#" + requestId + ") " +
                        "đã được xử lý xong. Vui lòng xác nhận kết quả trong mục Yêu cầu hỗ trợ.";
                type    = Notification.NotificationType.INFO;
            }
            case "REJECTED" -> {
                title   = "Yêu cầu hỗ trợ bị từ chối";
                content = "Yêu cầu \"" + requestTitle + "\" (#" + requestId + ") " +
                        "không thể thực hiện.";
                if (note != null && !note.isBlank())
                    content += " Lý do: " + note;
                type = Notification.NotificationType.WARNING;
            }
            default -> {
                title   = "Cập nhật yêu cầu hỗ trợ";
                content = "Yêu cầu #" + requestId + " đã được cập nhật trạng thái: " + newStatus;
            }
        }

        Notification n = new Notification();
        n.setId(generateId());
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setResident(resident);
        n.setApartment(resident.getApartment());
        n.setBuilding(resident.getApartment() != null ? resident.getApartment().getBuilding() : null);
        n.setCreatedBy(staff);
        n.setIsRead(false);
        notifRepo.save(n);
        logger.info("Request status notification sent — requestId={}, status={}, residentId={}",
                requestId, newStatus, resident.getId());
    }

    /**
     * Gửi thông báo nhắc thanh toán hóa đơn quá hạn.
     * Gọi từ InvoiceManagementService / InvoiceScheduler.
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
        logger.info("Payment reminder sent to resident {} for invoice {}", resident.getId(), invoiceId);
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
        m.put("id",        n.getId());
        m.put("title",     n.getTitle());
        m.put("content",   n.getContent());
        m.put("type",      n.getType().name());
        m.put("isRead",    n.getIsRead());
        m.put("createdAt", n.getCreatedAt().toString());
        m.put("createdBy", n.getCreatedBy() != null ? n.getCreatedBy().getFullName() : "Hệ thống");

        if (n.getResident() != null) {
            m.put("sendType",     "PERSONAL");
            m.put("residentId",   n.getResident().getId());
            m.put("residentName", n.getResident().getFullName());
        } else if (n.getApartment() != null) {
            m.put("sendType",        "APARTMENT");
            m.put("apartmentId",     n.getApartment().getId());
            m.put("apartmentNumber", n.getApartment().getNumber());
        } else {
            m.put("sendType", "BROADCAST");
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
        return "NTF" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}