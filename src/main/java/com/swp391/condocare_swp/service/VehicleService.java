package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import com.swp391.condocare_swp.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VehicleService — Quản lý đăng ký phương tiện.
 *
 * THAY ĐỔI (so với phiên bản cũ):
 *   1. Bỏ @Autowired NotificationRepository notifRepo
 *      → Inject NotificationService để gọi sendVehicleApprovedNotification() / sendVehicleRejectedNotification()
 *      → Tránh bypass NotificationService, đảm bảo nhất quán với các service khác
 *
 *   2. Bỏ private getCurrentResident() và getCurrentStaff() tự viết
 *      → Dùng SecurityUtils.getCurrentResident() / SecurityUtils.getCurrentStaff()
 *      → Loại bỏ trùng lặp SecurityContextHolder ở 10+ service
 *
 *   3. Bỏ private sendVehicleNotification() chứa logic tạo Notification thủ công
 *      → Thay bằng: notificationService.sendVehicleApprovedNotification() / sendVehicleRejectedNotification()
 */
@Service
public class VehicleService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private VehicleRepository   vehicleRepo;
    @Autowired private ResidentsRepository residentRepo;
    @Autowired private ApartmentRepository apartmentRepo;
    @Autowired private StaffRepository     staffRepo;

    // [THAY ĐỔI] Bỏ NotificationRepository, inject NotificationService thay thế
    @Autowired private NotificationService notificationService;

    // [THAY ĐỔI] Dùng SecurityUtils thay vì tự viết getCurrentResident/getCurrentStaff
    @Autowired private SecurityUtils securityUtils;

    // ─── STAFF: Thống kê ──────────────────────────────────────────────────────

    /**
     * Trả về thống kê tổng quan cho trang Quản lý xe phía Staff.
     * Frontend (vehicles.html) gọi GET /api/staff/vehicles/stats.
     * Response: { pending, approved, rejected, total }
     */
    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",    vehicleRepo.count());
        m.put("pending",  vehicleRepo.findByPendingStatus(Vehicle.PendingStatus.PENDING).size());
        m.put("approved", vehicleRepo.findByPendingStatus(Vehicle.PendingStatus.APPROVED).size());
        m.put("rejected", vehicleRepo.findByPendingStatus(Vehicle.PendingStatus.REJECTED).size());
        return m;
    }

    // ─── RESIDENT: Đăng ký xe mới ─────────────────────────────────────────────

    /**
     * Resident gửi yêu cầu đăng ký chỗ để xe.
     * Xe sẽ ở trạng thái PENDING cho đến khi Staff duyệt.
     */
    @Transactional
    public String registerVehicle(Map<String, String> body) {
        // [THAY ĐỔI] securityUtils.getCurrentResident() thay vì private method tự viết
        Residents resident = securityUtils.getCurrentResident();

        if (resident.getStatus() != Residents.ResidentStatus.ACTIVE)
            throw new RuntimeException("Tài khoản chưa được kích hoạt, không thể đăng ký xe.");
        if (resident.getApartment() == null)
            throw new RuntimeException("Bạn chưa được gán căn hộ, không thể đăng ký xe.");

        String type         = body.get("type");
        String licensePlate = body.get("licensePlate");
        String brand        = body.get("brand");
        String model        = body.get("model");
        String color        = body.get("color");
        String duration     = body.getOrDefault("durationType", "MONTHLY");

        if (type == null || type.isBlank())
            throw new RuntimeException("Loại xe không được để trống.");

        // Kiểm tra biển số trùng (chỉ với xe có biển số)
        if (licensePlate != null && !licensePlate.isBlank()
                && vehicleRepo.existsByLicensePlate(licensePlate.trim().toUpperCase()))
            throw new RuntimeException("Biển số xe '" + licensePlate + "' đã được đăng ký trong hệ thống.");

        // [FIX] valueOf() phải dùng toUpperCase() + try-catch để trả về message rõ ràng
        //       thay vì để JVM throw "No enum constant ..." khó hiểu cho client.
        Vehicle.VehicleType vehicleType;
        try {
            vehicleType = Vehicle.VehicleType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Loại xe không hợp lệ: '" + type + "'. Giá trị hợp lệ: MOTORBIKE, CAR, BICYCLE, ELECTRIC_BIKE, OTHER");
        }

        Vehicle.DurationType durType;
        try {
            durType = Vehicle.DurationType.valueOf(duration.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Loại thời hạn không hợp lệ: '" + duration + "'. Giá trị hợp lệ: MONTHLY, QUARTERLY, YEARLY");
        }

        Vehicle v = new Vehicle();
        v.setId(generateVehicleId());
        v.setType(vehicleType);
        v.setLicensePlate(licensePlate != null ? licensePlate.trim().toUpperCase() : null);
        v.setBrand(brand);
        v.setModel(model);
        v.setColor(color);
        v.setResident(resident);
        v.setApartment(resident.getApartment());
        v.setDurationType(durType);
        v.setPendingStatus(Vehicle.PendingStatus.PENDING);
        v.setStatus(Vehicle.VehicleStatus.ACTIVE);

        vehicleRepo.save(v);
        logger.info("Vehicle registration request {} by resident {}", v.getId(), resident.getId());
        return "Đăng ký xe thành công! Yêu cầu đang chờ Ban quản lý xét duyệt.";
    }

    // ─── RESIDENT: Xem danh sách xe của mình ──────────────────────────────────

    public List<Map<String, Object>> getMyVehicles() {
        // [THAY ĐỔI] securityUtils thay vì private method
        Residents resident = securityUtils.getCurrentResident();
        return vehicleRepo.findByResidentId(resident.getId())
                .stream().map(this::mapToResponse).toList();
    }

    // ─── STAFF: Danh sách xe chờ duyệt ───────────────────────────────────────

    public List<Map<String, Object>> getPendingVehicles() {
        return vehicleRepo.findByPendingStatus(Vehicle.PendingStatus.PENDING)
                .stream().map(this::mapToResponse).toList();
    }

    // ─── STAFF: Danh sách tất cả xe (có filter) ───────────────────────────────

    public List<Map<String, Object>> getAllVehicles(String type, String pendingStatus,
                                                    String status, String apartmentId) {
        Specification<Vehicle> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // [FIX] toUpperCase() để chịu được lowercase input từ frontend
            if (type != null && !type.isBlank())
                predicates.add(cb.equal(root.get("type"),
                        Vehicle.VehicleType.valueOf(type.toUpperCase())));

            if (pendingStatus != null && !pendingStatus.isBlank())
                predicates.add(cb.equal(root.get("pendingStatus"),
                        Vehicle.PendingStatus.valueOf(pendingStatus.toUpperCase())));

            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"),
                        Vehicle.VehicleStatus.valueOf(status.toUpperCase())));

            if (apartmentId != null && !apartmentId.isBlank())
                predicates.add(cb.equal(root.get("apartment").get("id"), apartmentId));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return vehicleRepo.findAll(spec).stream().map(this::mapToResponse).toList();
    }

    // ─── STAFF: Duyệt đăng ký xe ─────────────────────────────────────────────

    @Transactional
    public String approveVehicle(String vehicleId, String note) {
        Vehicle v = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe: " + vehicleId));
        if (v.getPendingStatus() != Vehicle.PendingStatus.PENDING)
            throw new RuntimeException("Xe này không ở trạng thái chờ duyệt.");

        // [THAY ĐỔI] securityUtils.getCurrentStaff()
        Staff staff = securityUtils.getCurrentStaff();
        v.setPendingStatus(Vehicle.PendingStatus.APPROVED);
        v.setApprovedBy(staff);
        v.setApprovedAt(LocalDateTime.now());
        v.setNote(note);
        v.setRegisteredAt(LocalDateTime.now());
        v.setExpiredAt(calculateExpiry(v.getDurationType()));

        // Cập nhật số lượng xe trong căn hộ
        Apartment apt = v.getApartment();
        apt.setTotalVehicle(apt.getTotalVehicle() + 1);
        apartmentRepo.save(apt);

        vehicleRepo.save(v);

        // [THAY ĐỔI] Gọi NotificationService thay vì tự save thông qua repo
        notificationService.sendVehicleApprovedNotification(v, staff);

        logger.info("Vehicle {} approved by staff {}", vehicleId, staff.getId());
        return "Đã duyệt đăng ký xe thành công!";
    }

    // ─── STAFF: Từ chối đăng ký xe ────────────────────────────────────────────

    @Transactional
    public String rejectVehicle(String vehicleId, String reason) {
        Vehicle v = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe: " + vehicleId));
        if (v.getPendingStatus() != Vehicle.PendingStatus.PENDING)
            throw new RuntimeException("Xe này không ở trạng thái chờ duyệt.");

        // [THAY ĐỔI] securityUtils.getCurrentStaff()
        Staff staff = securityUtils.getCurrentStaff();
        v.setPendingStatus(Vehicle.PendingStatus.REJECTED);
        v.setRejectReason(reason);
        v.setStatus(Vehicle.VehicleStatus.INACTIVE);
        vehicleRepo.save(v);

        // [THAY ĐỔI] Gọi NotificationService thay vì tự save thông qua repo
        notificationService.sendVehicleRejectedNotification(v, reason, staff);

        logger.info("Vehicle {} rejected by staff {} — {}", vehicleId, staff.getId(), reason);
        return "Đã từ chối đăng ký xe.";
    }

    // ─── STAFF: Thu hồi đăng ký xe ────────────────────────────────────────────

    @Transactional
    public String revokeVehicle(String vehicleId, String reason) {
        Vehicle v = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe: " + vehicleId));

        v.setStatus(Vehicle.VehicleStatus.REVOKED);
        v.setRevokedAt(LocalDateTime.now());
        v.setNote(reason);

        // Giảm số lượng xe trong căn hộ
        if (v.getPendingStatus() == Vehicle.PendingStatus.APPROVED) {
            Apartment apt = v.getApartment();
            apt.setTotalVehicle(Math.max(0, apt.getTotalVehicle() - 1));
            apartmentRepo.save(apt);
        }

        vehicleRepo.save(v);

        // [FIX #5] Gửi thông báo cho cư dân khi xe bị thu hồi
        // Trước đây không có notification → cư dân không biết xe bị thu hồi
        try {
            Staff staff = securityUtils.getCurrentStaff();
            notificationService.sendVehicleRevokedNotification(v, reason, staff);
        } catch (Exception e) {
            logger.warn("Could not send revoke notification for vehicle {}: {}", vehicleId, e.getMessage());
        }

        logger.info("Vehicle {} revoked — {}", vehicleId, reason);
        return "Đã thu hồi đăng ký xe thành công!";
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private LocalDateTime calculateExpiry(Vehicle.DurationType type) {
        return switch (type) {
            case MONTHLY   -> LocalDateTime.now().plusMonths(1);
            case QUARTERLY -> LocalDateTime.now().plusMonths(3);
            case YEARLY    -> LocalDateTime.now().plusYears(1);
        };
    }

    private Map<String, Object> mapToResponse(Vehicle v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            v.getId());
        m.put("type",          v.getType().name());
        m.put("typeLabel",     getTypeLabel(v.getType()));
        m.put("licensePlate",  v.getLicensePlate());
        m.put("brand",         v.getBrand());
        m.put("model",         v.getModel());
        m.put("color",         v.getColor());
        m.put("durationType",  v.getDurationType().name());
        m.put("pendingStatus", v.getPendingStatus().name());
        m.put("status",        v.getStatus().name());
        m.put("registeredAt",  v.getRegisteredAt());
        m.put("expiredAt",     v.getExpiredAt());
        m.put("rejectReason",  v.getRejectReason());
        m.put("note",          v.getNote());
        m.put("createdAt",     v.getCreatedAt());
        if (v.getResident() != null) {
            m.put("residentId",   v.getResident().getId());
            m.put("residentName", v.getResident().getFullName());
        }
        if (v.getApartment() != null) {
            m.put("apartmentId",     v.getApartment().getId());
            m.put("apartmentNumber", v.getApartment().getNumber());
        }
        if (v.getApprovedBy() != null) {
            m.put("approvedBy", v.getApprovedBy().getFullName());
            m.put("approvedAt", v.getApprovedAt());
        }
        return m;
    }

    private String getTypeLabel(Vehicle.VehicleType type) {
        return switch (type) {
            case MOTORBIKE     -> "xe máy";
            case CAR           -> "ô tô";
            case BICYCLE       -> "xe đạp";
            case ELECTRIC_BIKE -> "xe đạp điện";
            default            -> "phương tiện";
        };
    }

    private synchronized String generateVehicleId() {
        for (int i = 1; i <= 9999; i++) {
            String c = "VEH" + String.format("%03d", i);
            if (!vehicleRepo.existsById(c)) return c;
        }
        return "VEH" + System.currentTimeMillis() % 100000L + idCounter.incrementAndGet();
    }
}