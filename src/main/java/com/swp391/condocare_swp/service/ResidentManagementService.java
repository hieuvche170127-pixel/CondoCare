package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.ResidentCreateRequest;
import com.swp391.condocare_swp.dto.ResidentUpdateRequest;
import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ResidentManagementService — Quản lý cư dân phía Staff/Manager.
 *
 * Thay đổi so với phiên bản cũ:
 * - approveResident() và createResident() gọi NotificationService.sendAccountApprovedNotification()
 *   và sendAccessCardIssuedNotification() thay vì tự tạo Notification nội bộ.
 * - issueAccessCard() trả về cardNumber để NotificationService có thể đưa vào nội dung thông báo.
 * - Bỏ sendWelcomeNotification() nội bộ (đã chuyển sang NotificationService).
 */
@Service
public class ResidentManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ResidentManagementService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private ResidentsRepository    residentRepo;
    @Autowired private ApartmentRepository    apartmentRepo;
    @Autowired private StaffRepository        staffRepo;
    @Autowired private AccessCardRepository   accessCardRepo;
    @Autowired private PasswordEncoder        passwordEncoder;
    @Autowired private EmailService           emailService;
    @Autowired private NotificationService    notificationService;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    public Page<Map<String, Object>> listResidents(
            String search, String type, String status,
            String apartmentId, PageRequest pageable) {

        Specification<Residents> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String p = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), p),
                        cb.like(cb.lower(root.get("fullName")), p),
                        cb.like(cb.lower(root.get("email")),    p),
                        cb.like(cb.lower(root.get("phone")),    p),
                        cb.like(cb.lower(root.get("idNumber")), p)));
            }
            if (type != null && !type.isBlank())
                predicates.add(cb.equal(root.get("type"), Residents.ResidentType.valueOf(type)));
            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"), Residents.ResidentStatus.valueOf(status)));
            if (apartmentId != null && !apartmentId.isBlank())
                predicates.add(cb.equal(root.get("apartment").get("id"), apartmentId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return residentRepo.findAll(spec, pageable).map(this::mapToResponse);
    }

    // ─── CHI TIẾT ─────────────────────────────────────────────────────────────

    public Map<String, Object> getResidentDetail(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + id));
        Map<String, Object> res = mapToResponse(r);

        List<AccessCard> cards = accessCardRepo.findByResidentId(id);
        List<Map<String, Object>> cardList = cards.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",         c.getId());
            m.put("cardNumber", c.getCardNumber());
            m.put("status",     c.getStatus().name());
            m.put("issuedAt",   c.getIssuedAt());
            m.put("expiredAt",  c.getExpiredAt());
            return m;
        }).toList();
        res.put("accessCards", cardList);
        return res;
    }

    // ─── THỐNG KÊ ─────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",    residentRepo.count());
        m.put("pending",  residentRepo.countByStatus(Residents.ResidentStatus.PENDING));
        m.put("active",   residentRepo.countByStatus(Residents.ResidentStatus.ACTIVE));
        m.put("inactive", residentRepo.countByStatus(Residents.ResidentStatus.INACTIVE));
        m.put("owners",   residentRepo.countByType(Residents.ResidentType.OWNER));
        m.put("tenants",  residentRepo.countByType(Residents.ResidentType.TENANT));
        m.put("guests",   residentRepo.countByType(Residents.ResidentType.GUEST));
        return m;
    }

    // ─── DUYỆT TÀI KHOẢN PENDING ──────────────────────────────────────────────

    /**
     * Manager duyệt tài khoản PENDING.
     * Luồng sau duyệt:
     *   1. Gán căn hộ (nếu có)
     *   2. Cập nhật type (OWNER/TENANT/GUEST)
     *   3. Kích hoạt tài khoản (ACTIVE)
     *   4. Tự động cấp thẻ ra vào
     *   5. Gửi thông báo hệ thống (tài khoản được duyệt + thẻ được cấp)
     *   6. Gửi email
     */
    @Transactional
    public String approveResident(String residentId, String apartmentId,
                                  String type, String note) {
        Residents r = residentRepo.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + residentId));

        if (r.getStatus() != Residents.ResidentStatus.PENDING)
            throw new RuntimeException("Tài khoản này không ở trạng thái PENDING.");

        Staff verifier = getCurrentStaff();

        if (apartmentId != null && !apartmentId.isBlank()) {
            Apartment apt = apartmentRepo.findById(apartmentId)
                    .orElseThrow(() -> new RuntimeException("Căn hộ không tồn tại: " + apartmentId));
            r.setApartment(apt);
            apt.setTotalResident(apt.getTotalResident() + 1);
            apt.setStatus(Apartment.ApartmentStatus.OCCUPIED);
            apartmentRepo.save(apt);
        }

        if (type != null && !type.isBlank())
            r.setType(Residents.ResidentType.valueOf(type));

        r.setStatus(Residents.ResidentStatus.ACTIVE);
        r.setVerifiedBy(verifier);
        r.setVerifiedAt(LocalDateTime.now());
        residentRepo.save(r);

        // Cấp thẻ ra vào và lấy số thẻ để đưa vào thông báo
        String cardNumber = issueAccessCard(r, verifier);

        // Thông báo hệ thống: tài khoản được duyệt
        notificationService.sendAccountApprovedNotification(r, verifier);

        // Thông báo hệ thống: thẻ ra vào được cấp (chỉ khi thẻ mới được tạo)
        if (cardNumber != null) {
            notificationService.sendAccessCardIssuedNotification(r, cardNumber, verifier);
        }

        // Email
        if (r.getEmail() != null && !r.getEmail().isBlank()) {
            emailService.sendAccountApprovedEmail(r.getEmail(), r.getFullName());
        }

        logger.info("Resident {} approved by staff {}", residentId, verifier.getId());
        return "Đã duyệt tài khoản cư dân " + r.getFullName() + " thành công!";
    }

    /**
     * Manager từ chối tài khoản PENDING.
     */
    @Transactional
    public String rejectResident(String residentId, String reason) {
        Residents r = residentRepo.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + residentId));

        if (r.getStatus() != Residents.ResidentStatus.PENDING)
            throw new RuntimeException("Tài khoản này không ở trạng thái PENDING.");

        r.setStatus(Residents.ResidentStatus.INACTIVE);
        residentRepo.save(r);

        if (r.getEmail() != null && !r.getEmail().isBlank()) {
            emailService.sendAccountRejectedEmail(r.getEmail(), r.getFullName(), reason);
        }

        logger.info("Resident {} rejected — reason: {}", residentId, reason);
        return "Đã từ chối tài khoản cư dân " + r.getFullName() + ".";
    }

    // ─── TẠO CƯ DÂN (Manager tạo trực tiếp — ACTIVE ngay) ───────────────────

    @Transactional
    public String createResident(ResidentCreateRequest req) {
        validateCreateRequest(req);

        boolean autoGenerated = (req.getPassword() == null || req.getPassword().isBlank());
        String plainPassword  = autoGenerated ? generateRandomPassword() : req.getPassword();

        Residents r = new Residents();
        r.setId(generateResidentId());
        r.setUsername(req.getUsername().trim());
        r.setPassword(passwordEncoder.encode(plainPassword));
        r.setFullName(req.getFullName().trim());
        r.setType(Residents.ResidentType.valueOf(req.getType()));
        r.setDob(req.getDob());
        r.setGender(Residents.Gender.valueOf(req.getGender()));
        r.setIdNumber(req.getIdNumber());
        r.setPhone(req.getPhone());
        r.setEmail(req.getEmail());
        r.setStatus(Residents.ResidentStatus.ACTIVE);

        Staff verifier = getCurrentStaff();
        r.setVerifiedBy(verifier);
        r.setVerifiedAt(LocalDateTime.now());

        if (req.getApartmentId() != null && !req.getApartmentId().isBlank()) {
            Apartment apt = apartmentRepo.findById(req.getApartmentId())
                    .orElseThrow(() -> new RuntimeException("Căn hộ không tồn tại: " + req.getApartmentId()));
            r.setApartment(apt);
            apt.setTotalResident(apt.getTotalResident() + 1);
            apt.setStatus(Apartment.ApartmentStatus.OCCUPIED);
            apartmentRepo.save(apt);
        }

        residentRepo.save(r);

        // Cấp thẻ ra vào
        String cardNumber = issueAccessCard(r, verifier);

        // Thông báo hệ thống (tài khoản được tạo và thẻ được cấp)
        notificationService.sendAccountApprovedNotification(r, verifier);
        if (cardNumber != null) {
            notificationService.sendAccessCardIssuedNotification(r, cardNumber, verifier);
        }

        // Email
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            emailService.sendWelcomeEmail(req.getEmail(), r.getFullName(),
                    r.getUsername(), plainPassword, "cư dân");
        }

        logger.info("Resident {} created by staff {}", r.getId(), verifier.getId());
        return "Tạo cư dân thành công!" + (autoGenerated ? " Mật khẩu đã được gửi tới email." : "");
    }

    // ─── CẬP NHẬT ─────────────────────────────────────────────────────────────

    @Transactional
    public String updateResident(String id, ResidentUpdateRequest req) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + id));

        String newEmail = (req.getEmail() != null && req.getEmail().isBlank()) ? null : req.getEmail();
        if (newEmail != null && !newEmail.equals(r.getEmail())
                && residentRepo.existsByEmail(newEmail))
            throw new RuntimeException("Email đã được sử dụng bởi cư dân khác.");

        if (req.getFullName() != null && !req.getFullName().isBlank())
            r.setFullName(req.getFullName().trim());
        if (req.getType()     != null) r.setType(Residents.ResidentType.valueOf(req.getType()));
        if (req.getDob()      != null) r.setDob(req.getDob());
        if (req.getGender()   != null) r.setGender(Residents.Gender.valueOf(req.getGender()));
        if (req.getIdNumber() != null) r.setIdNumber(blankToNull(req.getIdNumber()));
        if (req.getPhone()    != null) {
            String ph = blankToNull(req.getPhone());
            if (ph == null) throw new RuntimeException("Số điện thoại không được để trống.");
            r.setPhone(ph);
        }
        r.setEmail(newEmail);
        if (req.getStatus()      != null) r.setStatus(Residents.ResidentStatus.valueOf(req.getStatus()));
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank())
            r.setPassword(passwordEncoder.encode(req.getNewPassword()));

        if (req.getApartmentId() != null) {
            Apartment oldApt = r.getApartment();
            if (req.getApartmentId().isBlank()) {
                if (oldApt != null) decreaseApartmentResident(oldApt);
                r.setApartment(null);
            } else {
                Apartment newApt = apartmentRepo.findById(req.getApartmentId())
                        .orElseThrow(() -> new RuntimeException("Căn hộ không tồn tại."));
                if (oldApt == null || !oldApt.getId().equals(newApt.getId())) {
                    if (oldApt != null) decreaseApartmentResident(oldApt);
                    newApt.setTotalResident(newApt.getTotalResident() + 1);
                    newApt.setStatus(Apartment.ApartmentStatus.OCCUPIED);
                    apartmentRepo.save(newApt);
                }
                r.setApartment(newApt);
            }
        }

        residentRepo.save(r);
        logger.info("Updated resident: {}", id);
        return "Cập nhật cư dân thành công!";
    }

    // ─── VÔ HIỆU HÓA ──────────────────────────────────────────────────────────

    @Transactional
    public String deactivateResident(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + id));
        r.setStatus(Residents.ResidentStatus.INACTIVE);

        // Khóa tất cả thẻ ra vào
        List<AccessCard> cards = accessCardRepo.findByResidentId(id);
        cards.stream()
                .filter(c -> c.getStatus() == AccessCard.CardStatus.ACTIVE)
                .forEach(c -> {
                    c.setStatus(AccessCard.CardStatus.BLOCKED);
                    accessCardRepo.save(c);
                });

        residentRepo.save(r);
        logger.info("Deactivated resident {} — {} access card(s) blocked", id, cards.size());
        return "Đã vô hiệu hóa tài khoản cư dân!";
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    /**
     * Tự động cấp thẻ ra vào.
     * Trả về cardNumber nếu thẻ mới được tạo, null nếu đã có thẻ ACTIVE rồi.
     */
    private String issueAccessCard(Residents resident, Staff issuedBy) {
        long activeCards = accessCardRepo.countByResidentIdAndStatus(
                resident.getId(), AccessCard.CardStatus.ACTIVE);
        if (activeCards > 0) {
            logger.info("Resident {} already has active access card, skipping", resident.getId());
            return null;
        }

        String cardNumber = generateCardNumber();
        AccessCard card = new AccessCard();
        card.setId(generateCardId());
        card.setCardNumber(cardNumber);
        card.setResident(resident);
        card.setIssuedBy(issuedBy);
        card.setIssuedAt(LocalDateTime.now());
        card.setExpiredAt(LocalDateTime.now().plusYears(2));
        card.setStatus(AccessCard.CardStatus.ACTIVE);
        accessCardRepo.save(card);

        logger.info("AccessCard {} issued to resident {}", cardNumber, resident.getId());
        return cardNumber;
    }

    private Staff getCurrentStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return staffRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Staff đang đăng nhập."));
    }

    private void decreaseApartmentResident(Apartment apt) {
        int newCount = Math.max(0, apt.getTotalResident() - 1);
        apt.setTotalResident(newCount);
        if (newCount == 0) apt.setStatus(Apartment.ApartmentStatus.EMPTY);
        apartmentRepo.save(apt);
    }

    private void validateCreateRequest(ResidentCreateRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new RuntimeException("Username không được để trống.");
        if (req.getFullName() == null || req.getFullName().isBlank())
            throw new RuntimeException("Họ và tên không được để trống.");
        if (req.getPhone() == null || req.getPhone().isBlank())
            throw new RuntimeException("Số điện thoại không được để trống.");
        if (req.getGender() == null || req.getGender().isBlank())
            throw new RuntimeException("Giới tính không được để trống.");
        if (req.getType() == null || req.getType().isBlank())
            throw new RuntimeException("Loại cư dân không được để trống.");
        if (residentRepo.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username '" + req.getUsername() + "' đã tồn tại.");
        if (req.getEmail() != null && !req.getEmail().isBlank()
                && residentRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi cư dân khác.");
    }

    private Map<String, Object> mapToResponse(Residents r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",        r.getId());
        m.put("username",  r.getUsername());
        m.put("fullName",  r.getFullName());
        m.put("type",      r.getType().name());
        m.put("dob",       r.getDob());
        m.put("gender",    r.getGender().name());
        m.put("idNumber",  r.getIdNumber());
        m.put("phone",     r.getPhone());
        m.put("email",     r.getEmail());
        m.put("status",    r.getStatus().name());
        m.put("lastLogin", r.getLastLogin());
        m.put("createdAt", r.getCreatedAt());
        m.put("verifiedAt", r.getVerifiedAt());
        m.put("verifiedBy", r.getVerifiedBy() != null ? r.getVerifiedBy().getFullName() : null);
        if (r.getApartment() != null) {
            m.put("apartmentId",     r.getApartment().getId());
            m.put("apartmentNumber", r.getApartment().getNumber());
            m.put("buildingName",    r.getApartment().getBuilding() != null
                    ? r.getApartment().getBuilding().getName() : "");
        } else {
            m.put("apartmentId",     null);
            m.put("apartmentNumber", null);
            m.put("buildingName",    null);
        }
        return m;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private synchronized String generateResidentId() {
        for (int i = 1; i <= 9999; i++) {
            String c = "RES" + String.format("%03d", i);
            if (!residentRepo.existsById(c)) return c;
        }
        return "RES" + (System.currentTimeMillis() % 100000L) + idCounter.incrementAndGet();
    }

    private synchronized String generateCardId() {
        for (int i = 1; i <= 9999; i++) {
            String c = "ACS" + String.format("%03d", i);
            if (!accessCardRepo.existsById(c)) return c;
        }
        return "ACS" + System.currentTimeMillis();
    }

    private String generateCardNumber() {
        SecureRandom rng = new SecureRandom();
        String candidate;
        do {
            candidate = String.format("CARD%04d", rng.nextInt(10000));
        } while (accessCardRepo.existsByCardNumber(candidate));
        return candidate;
    }

    // ─── RESET MẬT KHẨU ──────────────────────────────────────────────────────

    /**
     * Sinh mật khẩu random, lưu DB, gửi email cho cư dân.
     * Ném RuntimeException nếu cư dân không có email (không gửi được).
     */
    @Transactional
    public String resetPassword(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + id));

        if (r.getEmail() == null || r.getEmail().isBlank())
            throw new RuntimeException(
                    "Cư dân " + r.getFullName() + " chưa có địa chỉ email. Vui lòng cập nhật email trước.");

        String newPassword = generateRandomPassword();
        r.setPassword(passwordEncoder.encode(newPassword));
        residentRepo.save(r);

        logger.info("Password reset for resident [{}] by admin", id);

        // Gửi email thông báo mật khẩu mới
        emailService.sendWelcomeEmail(
                r.getEmail(),
                r.getFullName(),
                r.getUsername(),
                newPassword,
                "cư dân (mật khẩu mới)"
        );

        return "Đã đặt lại mật khẩu và gửi về email: " + r.getEmail();
    }

    private String generateRandomPassword() {
        final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";
        SecureRandom rng = new SecureRandom();
        char[] chars = new char[12];
        for (int i = 0; i < 12; i++) chars[i] = CHARS.charAt(rng.nextInt(CHARS.length()));
        return new String(chars);
    }
}