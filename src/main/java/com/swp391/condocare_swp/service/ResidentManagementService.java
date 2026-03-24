package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.ResidentCreateRequest;
import com.swp391.condocare_swp.dto.ResidentUpdateRequest;
import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ResidentManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ResidentManagementService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private ResidentsRepository residentRepo;
    @Autowired private ApartmentRepository  apartmentRepo;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private EmailService         emailService;

    /* ── LIST ── */
    public Page<Map<String, Object>> listResidents(
            String search, String type, String status,
            String apartmentId, PageRequest pageable) {

        Specification<Residents> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String p = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")),  p),
                        cb.like(cb.lower(root.get("fullName")),  p),
                        cb.like(cb.lower(root.get("email")),     p),
                        cb.like(cb.lower(root.get("phone")),     p),
                        cb.like(cb.lower(root.get("idNumber")),  p)
                ));
            }
            if (type != null && !type.isBlank())
                predicates.add(cb.equal(root.get("type"),
                        Residents.ResidentType.valueOf(type)));
            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"),
                        Residents.ResidentStatus.valueOf(status)));
            if (apartmentId != null && !apartmentId.isBlank())
                predicates.add(cb.equal(root.get("apartment").get("id"), apartmentId));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return residentRepo.findAll(spec, pageable).map(this::mapToResponse);
    }

    /* ── CHI TIẾT ── */
    public Map<String, Object> getResidentDetail(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + id));
        return mapToResponse(r);
    }

    /* ── THỐNG KÊ ── */
    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",    residentRepo.count());
        m.put("active",   residentRepo.countByStatus(Residents.ResidentStatus.ACTIVE));
        m.put("inactive", residentRepo.countByStatus(Residents.ResidentStatus.INACTIVE));
        m.put("owners",   residentRepo.countByType(Residents.ResidentType.OWNER));
        m.put("tenants",  residentRepo.countByType(Residents.ResidentType.TENANT));
        m.put("guests",   residentRepo.countByType(Residents.ResidentType.GUEST));
        return m;
    }

    /* ── TẠO MỚI ── */
    @Transactional
    public String createResident(ResidentCreateRequest req) {
        // Validate các trường bắt buộc
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new RuntimeException("Username không được để trống");
        if (req.getFullName() == null || req.getFullName().isBlank())
            throw new RuntimeException("Họ và tên không được để trống");
        if (req.getPhone() == null || req.getPhone().isBlank())
            throw new RuntimeException("Số điện thoại không được để trống");
        if (req.getGender() == null || req.getGender().isBlank())
            throw new RuntimeException("Giới tính không được để trống");
        if (req.getType() == null || req.getType().isBlank())
            throw new RuntimeException("Loại cư dân không được để trống");

        if (residentRepo.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username '" + req.getUsername() + "' đã tồn tại");

        // Chỉ kiểm tra email trùng khi email được cung cấp và không rỗng
        if (req.getEmail() != null && !req.getEmail().isBlank()
                && residentRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi cư dân khác");

        // Tự sinh password nếu không được cung cấp
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

        // [FIX] Gán căn hộ và cập nhật đồng bộ Apartment
        if (req.getApartmentId() != null && !req.getApartmentId().isBlank()) {
            Apartment apt = apartmentRepo.findById(req.getApartmentId())
                    .orElseThrow(() -> new RuntimeException("Căn hộ không tồn tại: " + req.getApartmentId()));
            r.setApartment(apt);

            // Cập nhật ngược lại Apartment
            apt.setTotalResident(apt.getTotalResident() + 1);
            apt.setStatus(Apartment.ApartmentStatus.OCCUPIED);
            apartmentRepo.save(apt);
            logger.info("Apartment {} status → OCCUPIED, totalResident → {}",
                    apt.getId(), apt.getTotalResident());
        }

        residentRepo.save(r);
        logger.info("Created resident: {} ({}) — password {}",
                r.getId(), r.getUsername(), autoGenerated ? "auto-generated" : "provided");

        // Gửi email chào mừng nếu có địa chỉ email
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            emailService.sendWelcomeEmail(
                    req.getEmail(),
                    r.getFullName(),
                    r.getUsername(),
                    plainPassword,   // plaintext — chỉ gửi 1 lần duy nhất
                    "cư dân"
            );
        }

        return "Tạo cư dân thành công!"
                + (autoGenerated ? " Mật khẩu đã được gửi tới email." : "");
    }

    /* ── CẬP NHẬT ── */
    @Transactional
    public String updateResident(String id, ResidentUpdateRequest req) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + id));

        // Chuẩn hoá email: chuỗi rỗng ("") = người dùng muốn xoá email → treat như null
        String newEmail = (req.getEmail() != null && req.getEmail().isBlank())
                ? null : req.getEmail();

        // Chỉ kiểm tra trùng khi email mới khác email cũ VÀ không rỗng
        if (newEmail != null && !newEmail.equals(r.getEmail())
                && residentRepo.existsByEmail(newEmail))
            throw new RuntimeException("Email đã được sử dụng bởi cư dân khác");

        if (req.getFullName() != null && !req.getFullName().isBlank())
            r.setFullName(req.getFullName().trim());
        if (req.getType()   != null) r.setType(Residents.ResidentType.valueOf(req.getType()));
        if (req.getDob()    != null) r.setDob(req.getDob());
        if (req.getGender() != null) r.setGender(Residents.Gender.valueOf(req.getGender()));

        String newIdNumber = blankToNull(req.getIdNumber());
        String newPhone    = blankToNull(req.getPhone());
        String newTempRes  = blankToNull(req.getTempResidence());
        String newTempAbs  = blankToNull(req.getTempAbsence());

        // idNumber: chỉ update nếu client gửi lên (không phải field bị bỏ qua hoàn toàn)
        if (req.getIdNumber() != null) r.setIdNumber(newIdNumber);
        // phone: chỉ set null nếu client chủ động xoá, giữ nguyên nếu không gửi
        if (req.getPhone() != null) {
            if (newPhone == null) throw new RuntimeException("Số điện thoại không được để trống");
            r.setPhone(newPhone);
        }
        // Dùng newEmail đã chuẩn hoá thay vì req.getEmail() trực tiếp
        r.setEmail(newEmail);
        if (req.getStatus() != null) r.setStatus(Residents.ResidentStatus.valueOf(req.getStatus()));
        if (req.getTempResidence() != null) r.setTempResidence(newTempRes);
        if (req.getTempAbsence()   != null) r.setTempAbsence(newTempAbs);
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank())
            r.setPassword(passwordEncoder.encode(req.getNewPassword()));

        // [FIX] Xử lý apartment: null = không đổi, "" = bỏ liên kết, id = đổi căn hộ
        if (req.getApartmentId() != null) {
            Apartment oldApt = r.getApartment(); // căn hộ cũ (có thể null)

            if (req.getApartmentId().isBlank()) {
                // Bỏ liên kết → giảm total_resident và reset status căn hộ cũ nếu cần
                if (oldApt != null) {
                    decreaseApartmentResident(oldApt);
                }
                r.setApartment(null);

            } else {
                Apartment newApt = apartmentRepo.findById(req.getApartmentId())
                        .orElseThrow(() -> new RuntimeException("Căn hộ không tồn tại"));

                boolean isSameApt = oldApt != null && oldApt.getId().equals(newApt.getId());
                if (!isSameApt) {
                    // Giảm căn hộ cũ
                    if (oldApt != null) {
                        decreaseApartmentResident(oldApt);
                    }
                    // Tăng căn hộ mới
                    newApt.setTotalResident(newApt.getTotalResident() + 1);
                    newApt.setStatus(Apartment.ApartmentStatus.OCCUPIED);
                    apartmentRepo.save(newApt);
                    logger.info("Apartment {} status → OCCUPIED, totalResident → {}",
                            newApt.getId(), newApt.getTotalResident());
                }
                r.setApartment(newApt);
            }
        }

        residentRepo.save(r);
        logger.info("Updated resident: {}", id);
        return "Cập nhật cư dân thành công!";
    }

    /* ── VÔ HIỆU HÓA (soft delete) ── */
    @Transactional
    public String deactivateResident(String id) {
        Residents r = residentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân: " + id));
        r.setStatus(Residents.ResidentStatus.INACTIVE);
        residentRepo.save(r);
        logger.info("Deactivated resident: {}", id);
        return "Đã vô hiệu hóa tài khoản cư dân!";
    }

    /* ── HELPERS ── */

    /**
     * Giảm total_resident của căn hộ đi 1.
     * Nếu về 0 thì đổi status về EMPTY.
     */
    private void decreaseApartmentResident(Apartment apt) {
        int newCount = Math.max(0, apt.getTotalResident() - 1);
        apt.setTotalResident(newCount);
        if (newCount == 0) {
            apt.setStatus(Apartment.ApartmentStatus.EMPTY);
            logger.info("Apartment {} status → EMPTY", apt.getId());
        }
        apartmentRepo.save(apt);
        logger.info("Apartment {} totalResident → {}", apt.getId(), newCount);
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
        m.put("tempResidence", r.getTempResidence());
        m.put("tempAbsence",   r.getTempAbsence());
        m.put("lastLogin", r.getLastLogin());
        m.put("createAt",  r.getCreateAt());
        if (r.getApartment() != null) {
            m.put("apartmentId",     r.getApartment().getId());
            m.put("apartmentNumber", r.getApartment().getNumber());
            m.put("buildingName",    r.getApartment().getBuilding() != null
                    ? r.getApartment().getBuilding().getName() : "");
        } else {
            m.put("apartmentId", null);
            m.put("apartmentNumber", null);
            m.put("buildingName", null);
        }
        return m;
    }

    /** Trả về null nếu chuỗi null hoặc rỗng/khoảng trắng, ngược lại trả về chuỗi đã trim. */
    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private synchronized String generateResidentId() {
        for (int i = 1; i <= 9999; i++) {
            String candidate = "RES" + String.format("%03d", i);
            if (!residentRepo.existsById(candidate)) return candidate;
        }
        return "RES" + (System.currentTimeMillis() % 100000L) + idCounter.incrementAndGet();
    }

    /**
     * Sinh password ngẫu nhiên 12 ký tự an toàn.
     * Gồm chữ hoa, chữ thường, số, ký tự đặc biệt — loại bỏ ký tự dễ nhầm.
     */
    private String generateRandomPassword() {
        final String UPPER   = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String LOWER   = "abcdefghjkmnpqrstuvwxyz";
        final String DIGITS  = "23456789";
        final String SPECIAL = "@#$%&*!";
        final String ALL     = UPPER + LOWER + DIGITS + SPECIAL;

        SecureRandom rng = new SecureRandom();
        char[] chars = new char[12];

        // Đảm bảo có đủ mỗi loại
        chars[0] = UPPER.charAt(rng.nextInt(UPPER.length()));
        chars[1] = LOWER.charAt(rng.nextInt(LOWER.length()));
        chars[2] = DIGITS.charAt(rng.nextInt(DIGITS.length()));
        chars[3] = SPECIAL.charAt(rng.nextInt(SPECIAL.length()));
        for (int i = 4; i < 12; i++)
            chars[i] = ALL.charAt(rng.nextInt(ALL.length()));

        // Xáo trộn
        for (int i = 11; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }
        return new String(chars);
    }
}