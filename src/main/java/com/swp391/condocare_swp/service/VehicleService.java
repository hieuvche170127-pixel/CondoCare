package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VehicleService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private VehicleRepository      vehicleRepo;
    @Autowired private ResidentsRepository    residentRepo;
    @Autowired private ApartmentRepository    apartmentRepo;
    @Autowired private StaffRepository        staffRepo;
    @Autowired private NotificationRepository notifRepo;

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
        Residents resident = getCurrentResident();

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

        Vehicle v = new Vehicle();
        v.setId(generateVehicleId());
        v.setType(Vehicle.VehicleType.valueOf(type));
        v.setLicensePlate(licensePlate != null ? licensePlate.trim().toUpperCase() : null);
        v.setBrand(brand);
        v.setModel(model);
        v.setColor(color);
        v.setResident(resident);
        v.setApartment(resident.getApartment());
        v.setDurationType(Vehicle.DurationType.valueOf(duration));
        v.setPendingStatus(Vehicle.PendingStatus.PENDING);
        v.setStatus(Vehicle.VehicleStatus.ACTIVE);

        vehicleRepo.save(v);
        logger.info("Vehicle registration request {} by resident {}", v.getId(), resident.getId());
        return "Đăng ký xe thành công! Yêu cầu đang chờ Ban quản lý xét duyệt.";
    }

    // ─── RESIDENT: Xem danh sách xe của mình ──────────────────────────────────

    public List<Map<String, Object>> getMyVehicles() {
        Residents resident = getCurrentResident();
        return vehicleRepo.findByResidentId(resident.getId())
                .stream().map(this::mapToResponse).toList();
    }

    // ─── STAFF: Danh sách xe chờ duyệt ───────────────────────────────────────

    public List<Map<String, Object>> getPendingVehicles() {
        return vehicleRepo.findByPendingStatus(Vehicle.PendingStatus.PENDING)
                .stream().map(this::mapToResponse).toList();
    }

    // ─── STAFF: Danh sách tất cả xe (có filter) ───────────────────────────────

    /**
     * Lấy danh sách xe với các bộ lọc tuỳ chọn.
     *
     * @param type          Loại xe: MOTORBIKE | CAR | BICYCLE | ELECTRIC_BIKE | OTHER
     * @param pendingStatus Trạng thái duyệt: PENDING | APPROVED | REJECTED
     * @param status        Trạng thái xe: ACTIVE | INACTIVE | REVOKED | LOST
     * @param apartmentId   Lọc theo căn hộ
     */
    public List<Map<String, Object>> getAllVehicles(String type, String pendingStatus,
                                                    String status, String apartmentId) {
        Specification<Vehicle> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (type != null && !type.isBlank())
                predicates.add(cb.equal(root.get("type"),
                        Vehicle.VehicleType.valueOf(type)));

            if (pendingStatus != null && !pendingStatus.isBlank())
                predicates.add(cb.equal(root.get("pendingStatus"),
                        Vehicle.PendingStatus.valueOf(pendingStatus)));

            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"),
                        Vehicle.VehicleStatus.valueOf(status)));

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

        Staff staff = getCurrentStaff();
        v.setPendingStatus(Vehicle.PendingStatus.APPROVED);
        v.setApprovedBy(staff);
        v.setApprovedAt(LocalDateTime.now());
        v.setNote(note);

        // Tính ngày hết hạn
        v.setRegisteredAt(LocalDateTime.now());
        v.setExpiredAt(calculateExpiry(v.getDurationType()));

        // Cập nhật số lượng xe trong căn hộ
        Apartment apt = v.getApartment();
        apt.setTotalVehicle(apt.getTotalVehicle() + 1);
        apartmentRepo.save(apt);

        vehicleRepo.save(v);

        // Gửi thông báo cho resident
        sendVehicleNotification(v, true, null, staff);

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

        Staff staff = getCurrentStaff();
        v.setPendingStatus(Vehicle.PendingStatus.REJECTED);
        v.setRejectReason(reason);
        v.setStatus(Vehicle.VehicleStatus.INACTIVE);
        vehicleRepo.save(v);

        // Gửi thông báo từ chối
        sendVehicleNotification(v, false, reason, staff);

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

    private void sendVehicleNotification(Vehicle v, boolean approved,
                                         String reason, Staff staff) {
        Notification notif = new Notification();
        notif.setId("NTF" + String.format("%012d", System.currentTimeMillis() % 1_000_000_000_000L));
        notif.setResident(v.getResident());
        notif.setCreatedBy(staff);
        notif.setIsRead(false);

        String plateInfo = v.getLicensePlate() != null ? " (" + v.getLicensePlate() + ")" : "";
        String typeLabel = getTypeLabel(v.getType());

        if (approved) {
            notif.setTitle("Đăng ký xe đã được duyệt");
            notif.setContent("Yêu cầu đăng ký " + typeLabel + plateInfo +
                    " của bạn đã được Ban quản lý phê duyệt. " +
                    "Hạn đăng ký: " + (v.getExpiredAt() != null
                    ? v.getExpiredAt().toLocalDate().toString() : "N/A") + ".");
            notif.setType(Notification.NotificationType.INFO);
        } else {
            notif.setTitle("Đăng ký xe bị từ chối");
            notif.setContent("Yêu cầu đăng ký " + typeLabel + plateInfo +
                    " của bạn bị từ chối. Lý do: " + (reason != null ? reason : "Không rõ."));
            notif.setType(Notification.NotificationType.WARNING);
        }

        notifRepo.save(notif);
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

    private Residents getCurrentResident() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return residentRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin cư dân."));
    }

    private Staff getCurrentStaff() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return staffRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Staff."));
    }

    private synchronized String generateVehicleId() {
        for (int i = 1; i <= 9999; i++) {
            String c = "VEH" + String.format("%03d", i);
            if (!vehicleRepo.existsById(c)) return c;
        }
        return "VEH" + System.currentTimeMillis() % 100000L + idCounter.incrementAndGet();
    }
}