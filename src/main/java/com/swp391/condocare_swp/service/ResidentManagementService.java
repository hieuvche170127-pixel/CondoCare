package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.ResidentCreateRequest;
import com.swp391.condocare_swp.dto.ResidentUpdateRequest;
import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import com.swp391.condocare_swp.security.SecurityUtils;
import com.swp391.condocare_swp.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
 * THAY ĐỔI (so với phiên bản cũ):
 *   1. Bỏ private generateRandomPassword() tự viết
 *      -> Dùng PasswordUtils.generateRandomPassword() (dùng chung với StaffManagementService)
 *   2. Bỏ private getCurrentStaff() (SecurityContextHolder inline)
 *      -> Dùng securityUtils.getCurrentStaff()
 */
@Service
public class ResidentManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ResidentManagementService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private ResidentsRepository  residentRepo;
    @Autowired private ApartmentRepository  apartmentRepo;
    @Autowired private StaffRepository      staffRepo;
    @Autowired private AccessCardRepository accessCardRepo;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private EmailService         emailService;
    @Autowired private NotificationService  notificationService;
    @Autowired private SecurityUtils        securityUtils;

    // LIST
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

    // CHI TIET
    public Map<String, Object> getResidentDetail(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay cu dan: " + id));
        Map<String, Object> res = mapToResponse(r);
        List<AccessCard> cards = accessCardRepo.findByResidentId(id);
        res.put("accessCards", cards.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",         c.getId());
            m.put("cardNumber", c.getCardNumber());
            m.put("status",     c.getStatus().name());
            m.put("issuedAt",   c.getIssuedAt());
            m.put("expiredAt",  c.getExpiredAt());
            return m;
        }).toList());
        return res;
    }

    // THONG KE
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

    // DUYET PENDING
    @Transactional
    public String approveResident(String residentId, String apartmentId, String type, String note) {
        Residents r = residentRepo.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay cu dan: " + residentId));
        if (r.getStatus() != Residents.ResidentStatus.PENDING)
            throw new RuntimeException("Tai khoan nay khong o trang thai PENDING.");

        Staff verifier = securityUtils.getCurrentStaff(); // [FIX]

        if (apartmentId != null && !apartmentId.isBlank()) {
            Apartment apt = apartmentRepo.findById(apartmentId)
                    .orElseThrow(() -> new RuntimeException("Can ho khong ton tai: " + apartmentId));
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

        String cardNumber = issueAccessCard(r, verifier);
        notificationService.sendAccountApprovedNotification(r, verifier);
        if (cardNumber != null)
            notificationService.sendAccessCardIssuedNotification(r, cardNumber, verifier);
        if (r.getEmail() != null && !r.getEmail().isBlank())
            emailService.sendAccountApprovedEmail(r.getEmail(), r.getFullName());

        logger.info("Resident {} approved by staff {}", residentId, verifier.getId());
        return "Da duyet tai khoan cu dan " + r.getFullName() + " thanh cong!";
    }

    @Transactional
    public String rejectResident(String residentId, String reason) {
        Residents r = residentRepo.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay cu dan: " + residentId));
        if (r.getStatus() != Residents.ResidentStatus.PENDING)
            throw new RuntimeException("Tai khoan nay khong o trang thai PENDING.");
        r.setStatus(Residents.ResidentStatus.INACTIVE);
        residentRepo.save(r);
        if (r.getEmail() != null && !r.getEmail().isBlank())
            emailService.sendAccountRejectedEmail(r.getEmail(), r.getFullName(), reason);
        logger.info("Resident {} rejected — reason: {}", residentId, reason);
        return "Da tu choi tai khoan cu dan " + r.getFullName() + ".";
    }

    // TAO CU DAN
    @Transactional
    public String createResident(ResidentCreateRequest req) {
        validateCreateRequest(req);
        boolean autoGenerated = (req.getPassword() == null || req.getPassword().isBlank());
        String plainPassword = autoGenerated ? PasswordUtils.generateRandomPassword() : req.getPassword(); // [FIX]

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

        Staff verifier = securityUtils.getCurrentStaff(); // [FIX]
        r.setVerifiedBy(verifier);
        r.setVerifiedAt(LocalDateTime.now());

        if (req.getApartmentId() != null && !req.getApartmentId().isBlank()) {
            Apartment apt = apartmentRepo.findById(req.getApartmentId())
                    .orElseThrow(() -> new RuntimeException("Can ho khong ton tai: " + req.getApartmentId()));
            r.setApartment(apt);
            apt.setTotalResident(apt.getTotalResident() + 1);
            apt.setStatus(Apartment.ApartmentStatus.OCCUPIED);
            apartmentRepo.save(apt);
        }
        residentRepo.save(r);

        String cardNumber = issueAccessCard(r, verifier);
        notificationService.sendAccountApprovedNotification(r, verifier);
        if (cardNumber != null)
            notificationService.sendAccessCardIssuedNotification(r, cardNumber, verifier);
        if (req.getEmail() != null && !req.getEmail().isBlank())
            emailService.sendWelcomeEmail(req.getEmail(), r.getFullName(), r.getUsername(), plainPassword, "cu dan");

        logger.info("Resident {} created by staff {}", r.getId(), verifier.getId());
        return "Tao cu dan thanh cong!" + (autoGenerated ? " Mat khau da duoc gui toi email." : "");
    }

    // CAP NHAT
    @Transactional
    public String updateResident(String id, ResidentUpdateRequest req) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay cu dan: " + id));
        String newEmail = (req.getEmail() != null && req.getEmail().isBlank()) ? null : req.getEmail();
        if (newEmail != null && !newEmail.equals(r.getEmail()) && residentRepo.existsByEmail(newEmail))
            throw new RuntimeException("Email da duoc su dung boi cu dan khac.");
        if (req.getFullName() != null && !req.getFullName().isBlank()) r.setFullName(req.getFullName().trim());
        if (req.getType()     != null) r.setType(Residents.ResidentType.valueOf(req.getType()));
        if (req.getDob()      != null) r.setDob(req.getDob());
        if (req.getGender()   != null) r.setGender(Residents.Gender.valueOf(req.getGender()));
        if (req.getIdNumber() != null) r.setIdNumber(blankToNull(req.getIdNumber()));
        if (req.getPhone()    != null) {
            String ph = blankToNull(req.getPhone());
            if (ph == null) throw new RuntimeException("So dien thoai khong duoc de trong.");
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
                        .orElseThrow(() -> new RuntimeException("Can ho khong ton tai."));
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
        return "Cap nhat cu dan thanh cong!";
    }

    // VO HIEU HOA
    @Transactional
    public String deactivateResident(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay cu dan: " + id));
        r.setStatus(Residents.ResidentStatus.INACTIVE);
        List<AccessCard> cards = accessCardRepo.findByResidentId(id);
        cards.stream().filter(c -> c.getStatus() == AccessCard.CardStatus.ACTIVE).forEach(c -> {
            c.setStatus(AccessCard.CardStatus.BLOCKED);
            accessCardRepo.save(c);
        });
        residentRepo.save(r);
        logger.info("Deactivated resident {} — {} access card(s) blocked", id, cards.size());
        return "Da vo hieu hoa tai khoan cu dan!";
    }

    // RESET MAT KHAU
    @Transactional
    public String resetPassword(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay cu dan: " + id));
        if (r.getEmail() == null || r.getEmail().isBlank())
            throw new RuntimeException("Cu dan " + r.getFullName() + " chua co dia chi email.");
        String newPassword = PasswordUtils.generateRandomPassword(); // [FIX]
        r.setPassword(passwordEncoder.encode(newPassword));
        residentRepo.save(r);
        logger.info("Password reset for resident [{}] by admin", id);
        emailService.sendWelcomeEmail(r.getEmail(), r.getFullName(), r.getUsername(), newPassword, "cu dan (mat khau moi)");
        return "Da dat lai mat khau va gui ve email: " + r.getEmail();
    }

    // HELPERS
    private String issueAccessCard(Residents resident, Staff issuedBy) {
        long activeCards = accessCardRepo.countByResidentIdAndStatus(resident.getId(), AccessCard.CardStatus.ACTIVE);
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

    private void decreaseApartmentResident(Apartment apt) {
        int newCount = Math.max(0, apt.getTotalResident() - 1);
        apt.setTotalResident(newCount);
        if (newCount == 0) apt.setStatus(Apartment.ApartmentStatus.EMPTY);
        apartmentRepo.save(apt);
    }

    private void validateCreateRequest(ResidentCreateRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) throw new RuntimeException("Username khong duoc de trong.");
        if (req.getFullName() == null || req.getFullName().isBlank()) throw new RuntimeException("Ho va ten khong duoc de trong.");
        if (req.getPhone() == null || req.getPhone().isBlank()) throw new RuntimeException("So dien thoai khong duoc de trong.");
        if (req.getGender() == null || req.getGender().isBlank()) throw new RuntimeException("Gioi tinh khong duoc de trong.");
        if (req.getType() == null || req.getType().isBlank()) throw new RuntimeException("Loai cu dan khong duoc de trong.");
        if (residentRepo.existsByUsername(req.getUsername())) throw new RuntimeException("Username '" + req.getUsername() + "' da ton tai.");
        if (req.getEmail() != null && !req.getEmail().isBlank() && residentRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email da duoc su dung boi cu dan khac.");
    }

    private Map<String, Object> mapToResponse(Residents r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         r.getId());
        m.put("username",   r.getUsername());
        m.put("fullName",   r.getFullName());
        m.put("type",       r.getType().name());
        m.put("dob",        r.getDob());
        m.put("gender",     r.getGender().name());
        m.put("idNumber",   r.getIdNumber());
        m.put("phone",      r.getPhone());
        m.put("email",      r.getEmail());
        m.put("status",     r.getStatus().name());
        m.put("lastLogin",  r.getLastLogin());
        m.put("createdAt",  r.getCreatedAt());
        m.put("verifiedAt", r.getVerifiedAt());
        m.put("verifiedBy", r.getVerifiedBy() != null ? r.getVerifiedBy().getFullName() : null);
        if (r.getApartment() != null) {
            m.put("apartmentId",     r.getApartment().getId());
            m.put("apartmentNumber", r.getApartment().getNumber());
            m.put("buildingName",    r.getApartment().getBuilding() != null ? r.getApartment().getBuilding().getName() : "");
        } else {
            m.put("apartmentId", null); m.put("apartmentNumber", null); m.put("buildingName", null);
        }
        return m;
    }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    private synchronized String generateResidentId() {
        for (int i = 1; i <= 9999; i++) { String c = "RES" + String.format("%03d", i); if (!residentRepo.existsById(c)) return c; }
        return "RES" + (System.currentTimeMillis() % 100000L) + idCounter.incrementAndGet();
    }

    private synchronized String generateCardId() {
        for (int i = 1; i <= 9999; i++) { String c = "ACS" + String.format("%03d", i); if (!accessCardRepo.existsById(c)) return c; }
        return "ACS" + System.currentTimeMillis();
    }

    private String generateCardNumber() {
        SecureRandom rng = new SecureRandom();
        String candidate;
        do { candidate = String.format("CARD%04d", rng.nextInt(10000)); } while (accessCardRepo.existsByCardNumber(candidate));
        return candidate;
    }
}