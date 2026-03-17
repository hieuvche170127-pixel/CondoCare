package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.StaffCreateRequest;
import com.swp391.condocare_swp.dto.StaffUpdateRequest;
import com.swp391.condocare_swp.entity.Role;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.RoleRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StaffManagementService {

    private static final Logger logger = LoggerFactory.getLogger(StaffManagementService.class);
    // Counter dùng để tránh trùng ID khi nhiều request đồng thời
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private StaffRepository staffRepo;
    @Autowired private RoleRepository  roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    /* ── LIST với search / filter / pagination ── */
    public Page<Map<String, Object>> listStaff(
            String search, String roleId, String status, PageRequest pageable) {

        Specification<Staff> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String p = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")),  p),
                        cb.like(cb.lower(root.get("fullName")),  p),
                        cb.like(cb.lower(root.get("email")),     p),
                        cb.like(cb.lower(root.get("phone")),     p),
                        cb.like(cb.lower(root.get("position")),  p),
                        cb.like(cb.lower(root.get("department")), p)
                ));
            }
            if (roleId != null && !roleId.isBlank())
                predicates.add(cb.equal(root.get("role").get("id"), roleId));
            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"), Staff.StaffStatus.valueOf(status)));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return staffRepo.findAll(spec, pageable).map(this::mapToResponse);
    }

    /* ── CHI TIẾT ── */
    public Map<String, Object> getStaffDetail(String id) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + id));
        return mapToResponse(staff);
    }

    /* ── TẤT CẢ ROLES (cho dropdown) ── */
    public List<Map<String, Object>> getAllRoles() {
        return roleRepo.findAll().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   r.getId());
            m.put("name", r.getName());
            m.put("description", r.getDescription());
            return m;
        }).toList();
    }

    /* ── THỐNG KÊ NHANH ── */
    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",   staffRepo.count());
        m.put("active",  staffRepo.countByStatus(Staff.StaffStatus.ACTIVE));
        m.put("resigned",staffRepo.countByStatus(Staff.StaffStatus.RESIGNED));
        m.put("onLeave", staffRepo.countByStatus(Staff.StaffStatus.ON_LEAVE));
        return m;
    }

    /* ── TẠO MỚI ── */
    @Transactional
    public String createStaff(StaffCreateRequest req) {
        if (staffRepo.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username '" + req.getUsername() + "' đã tồn tại");
        if (req.getEmail() != null && !req.getEmail().isBlank()
                && staffRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi nhân viên khác");


        Role role = roleRepo.findById(req.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + req.getRoleId()));

        Staff staff = new Staff();
        staff.setId(generateStaffId());
        staff.setUsername(req.getUsername().trim());
        staff.setPassword(passwordEncoder.encode(req.getPassword()));
        staff.setFullName(req.getFullName().trim());
        staff.setEmail(req.getEmail());
        staff.setPhone(req.getPhone());
        staff.setPosition(req.getPosition() != null ? req.getPosition() : "");
        staff.setDepartment(req.getDepartment() != null ? req.getDepartment() : "");
        staff.setDob(req.getDob());
        staff.setGender(Staff.Gender.valueOf(req.getGender()));
        staff.setRole(role);
        staff.setStatus(Staff.StaffStatus.ACTIVE);
        staff.setHiredAt(req.getHiredAt() != null ? req.getHiredAt() : LocalDateTime.now());

        staffRepo.save(staff);
        logger.info("Created staff: {} ({})", staff.getId(), staff.getUsername());
        return "Tạo nhân viên thành công!";
    }

    /* ── CẬP NHẬT ── */
    @Transactional
    public String updateStaff(String id, StaffUpdateRequest req) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + id));

        if (req.getEmail() != null && !req.getEmail().equals(staff.getEmail())
                && staffRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi nhân viên khác");

        if (req.getFullName()   != null) staff.setFullName(req.getFullName());
        if (req.getEmail()      != null) staff.setEmail(req.getEmail());
        if (req.getPhone()      != null) staff.setPhone(req.getPhone());
        if (req.getPosition()   != null) staff.setPosition(req.getPosition());
        if (req.getDepartment() != null) staff.setDepartment(req.getDepartment());
        if (req.getDob()        != null) staff.setDob(req.getDob());
        if (req.getGender()     != null) staff.setGender(Staff.Gender.valueOf(req.getGender()));
        if (req.getStatus()     != null) staff.setStatus(Staff.StaffStatus.valueOf(req.getStatus()));
        if (req.getRoleId()     != null) {
            Role role = roleRepo.findById(req.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            staff.setRole(role);
        }
        // Đổi password nếu có
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank())
            staff.setPassword(passwordEncoder.encode(req.getNewPassword()));

        staffRepo.save(staff);
        logger.info("Updated staff: {}", id);
        return "Cập nhật thành công!";
    }

    /* ── XÓA (soft delete) ── */
    @Transactional
    public String deleteStaff(String id) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + id));

        // Lấy username của người đang thực hiện hành động
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        // Không được tự vô hiệu hóa chính mình
        if (staff.getUsername().equals(currentUsername)) {
            throw new RuntimeException("Bạn không thể tự vô hiệu hóa tài khoản của mình!");
        }

        // Không được vô hiệu hóa tài khoản ADMIN khác
        // (chỉ áp dụng nếu role name là ADMIN)
        if ("ADMIN".equalsIgnoreCase(staff.getRole().getName())) {
            Staff currentStaff = staffRepo.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Không xác định được người thực hiện"));
            if (!"ADMIN".equalsIgnoreCase(currentStaff.getRole().getName())) {
                throw new RuntimeException("Chỉ ADMIN mới có thể vô hiệu hóa tài khoản ADMIN khác!");
            }
        }

        staff.setStatus(Staff.StaffStatus.RESIGNED);
        staffRepo.save(staff);
        logger.info("Soft-deleted staff: {} by {}", id, currentUsername);
        return "Đã vô hiệu hóa nhân viên thành công!";
    }

    /* ── HELPERS ── */
    private Map<String, Object> mapToResponse(Staff staff) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         staff.getId());
        m.put("username",   staff.getUsername());
        m.put("fullName",   staff.getFullName());
        m.put("email",      staff.getEmail());
        m.put("phone",      staff.getPhone());
        m.put("position",   staff.getPosition());
        m.put("department", staff.getDepartment());
        m.put("dob",        staff.getDob());
        m.put("gender",     staff.getGender().name());
        m.put("roleId",     staff.getRole().getId());
        m.put("roleName",   staff.getRole().getName());
        m.put("status",     staff.getStatus().name());
        m.put("hiredAt",    staff.getHiredAt());
        m.put("lastLogin",  staff.getLastLogin());
        m.put("createAt",   staff.getCreateAt());
        return m;
    }

    /**
     * Sinh ID dạng S001 ~ S999 an toàn, không trùng dù nhiều request đồng thời.
     * Nếu > 999 thì dùng timestamp để đảm bảo unique.
     */
    private synchronized String generateStaffId() {
        // Thử tìm ID chưa dùng bắt đầu từ S001
        for (int i = 1; i <= 999; i++) {
            String candidate = "S" + String.format("%03d", i);
            if (!staffRepo.existsById(candidate)) return candidate;
        }
        // Fallback: dùng timestamp + counter
        return "S" + (System.currentTimeMillis() % 100000L) + idCounter.incrementAndGet();
    }
}