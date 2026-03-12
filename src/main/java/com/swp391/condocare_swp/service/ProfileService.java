package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.ChangePasswordRequest;
import com.swp391.condocare_swp.dto.ProfileResponse;
import com.swp391.condocare_swp.dto.UpdateProfileRequest;
import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Fees;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.FeesRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired private StaffRepository     staffRepository;
    @Autowired private ResidentsRepository residentsRepository;
    @Autowired private FeesRepository      feesRepository;
    @Autowired private PasswordEncoder     passwordEncoder;

    /* ══════════════════════════════════════════════════════
       GET PROFILE
    ══════════════════════════════════════════════════════ */
    public ProfileResponse getProfile() {
        String username = currentUsername();
        logger.info("Getting profile for user: {}", username);

        Staff staff = staffRepository.findByUsername(username).orElse(null);
        if (staff != null) return buildStaffProfile(staff);

        Residents resident = residentsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return buildResidentProfile(resident);
    }

    /* ══════════════════════════════════════════════════════
       UPDATE PROFILE
    ══════════════════════════════════════════════════════ */
    @Transactional
    public String updateProfile(UpdateProfileRequest req) {
        String username = currentUsername();
        logger.info("Updating profile for user: {}", username);

        Staff staff = staffRepository.findByUsername(username).orElse(null);
        if (staff != null) return updateStaffProfile(staff, req);

        Residents resident = residentsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return updateResidentProfile(resident, req);
    }

    /* ══════════════════════════════════════════════════════
       CHANGE PASSWORD
    ══════════════════════════════════════════════════════ */
    @Transactional
    public String changePassword(ChangePasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword()))
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");

        String username = currentUsername();

        Staff staff = staffRepository.findByUsername(username).orElse(null);
        if (staff != null) {
            if (!passwordEncoder.matches(req.getCurrentPassword(), staff.getPassword()))
                throw new RuntimeException("Mật khẩu hiện tại không đúng");
            staff.setPassword(passwordEncoder.encode(req.getNewPassword()));
            staffRepository.save(staff);
            return "Đổi mật khẩu thành công!";
        }

        Residents resident = residentsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(req.getCurrentPassword(), resident.getPassword()))
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        resident.setPassword(passwordEncoder.encode(req.getNewPassword()));
        residentsRepository.save(resident);
        return "Đổi mật khẩu thành công!";
    }

    /* ══════════════════════════════════════════════════════
       PRIVATE — BUILD RESPONSE
    ══════════════════════════════════════════════════════ */
    private ProfileResponse buildStaffProfile(Staff s) {
        return ProfileResponse.builder()
                .id(s.getId())
                .username(s.getUsername())
                .fullName(s.getFullName())
                .email(s.getEmail())
                .phone(s.getPhone())
                .dob(s.getDob() != null ? s.getDob().toString() : null)
                .gender(s.getGender() != null ? s.getGender().name() : null)
                .userType("staff")
                .position(s.getPosition())
                .department(s.getDepartment())
                .staffStatus(s.getStatus() != null ? s.getStatus().name() : null)
                .roleName(s.getRole() != null ? s.getRole().getName() : null)
                .fees(new ArrayList<>())
                .build();
    }

    private ProfileResponse buildResidentProfile(Residents r) {
        ProfileResponse.ProfileResponseBuilder b = ProfileResponse.builder()
                .id(r.getId())
                .username(r.getUsername())
                .fullName(r.getFullName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .dob(r.getDob() != null ? r.getDob().toString() : null)
                .gender(r.getGender() != null ? r.getGender().name() : null)
                .userType("resident")
                .idNumber(r.getIdNumber())
                .residentType(r.getType() != null ? r.getType().name() : null)
                .residentStatus(r.getStatus() != null ? r.getStatus().name() : null)
                .tempResidence(r.getTempResidence())
                .tempAbsence(r.getTempAbsence());

        Apartment apt = r.getApartment();
        if (apt != null) {
            b.apartmentId(apt.getId())
                    .apartmentNumber(apt.getNumber())
                    .apartmentArea(apt.getArea() != null ? (double) apt.getArea() : null)
                    .buildingName(apt.getBuilding() != null ? apt.getBuilding().getName() : null);

            // Load tất cả fees của căn hộ (active + inactive parking)
            List<Fees> allFees = feesRepository.findByApartment(apt);
            List<ProfileResponse.FeeInfo> feeInfos = allFees.stream()
                    .map(f -> ProfileResponse.FeeInfo.builder()
                            .id(f.getId())
                            .name(f.getName())
                            .type(f.getType() != null ? f.getType().name() : "OTHER")
                            .vehicleType(detectVehicleType(f.getName()))
                            .amount(f.getAmount() != null ? f.getAmount().doubleValue() : 0)
                            .active(f.getEffectiveTo() == null) // null = đang dùng
                            .build())
                    .collect(Collectors.toList());
            b.fees(feeInfos);
        } else {
            b.fees(new ArrayList<>());
        }

        return b.build();
    }

    /* ══════════════════════════════════════════════════════
       PRIVATE — UPDATE
    ══════════════════════════════════════════════════════ */
    private String updateStaffProfile(Staff staff, UpdateProfileRequest req) {
        if (staff.getEmail() != null && !staff.getEmail().equals(req.getEmail())
                && staffRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");

        staff.setFullName(req.getFullName());
        staff.setEmail(req.getEmail());
        staff.setPhone(req.getPhone());
        if (req.getDob() != null && !req.getDob().isBlank())
            staff.setDob(LocalDate.parse(req.getDob()));
        if (req.getGender() != null && !req.getGender().isBlank())
            staff.setGender(Staff.Gender.valueOf(req.getGender().toUpperCase()));

        staffRepository.save(staff);
        logger.info("Staff profile updated: {}", staff.getUsername());
        return "Cập nhật thông tin thành công!";
    }

    private String updateResidentProfile(Residents resident, UpdateProfileRequest req) {
        if (resident.getEmail() != null && !resident.getEmail().equals(req.getEmail())
                && residentsRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");

        resident.setFullName(req.getFullName());
        resident.setEmail(req.getEmail());
        resident.setPhone(req.getPhone());
        if (req.getDob() != null && !req.getDob().isBlank())
            resident.setDob(LocalDate.parse(req.getDob()));
        if (req.getGender() != null && !req.getGender().isBlank())
            resident.setGender(Residents.Gender.valueOf(req.getGender().toUpperCase()));
        if (req.getIdNumber() != null)
            resident.setIdNumber(req.getIdNumber());
        if (req.getTempResidence() != null)
            resident.setTempResidence(req.getTempResidence());
        if (req.getTempAbsence() != null)
            resident.setTempAbsence(req.getTempAbsence());

        residentsRepository.save(resident);

        // Cập nhật trạng thái fees gửi xe nếu có gửi selectedFeeIds
        if (req.getSelectedFeeIds() != null && resident.getApartment() != null) {
            updateParkingFees(resident.getApartment(), req.getSelectedFeeIds());
        }

        logger.info("Resident profile updated: {}", resident.getUsername());
        return "Cập nhật thông tin thành công!";
    }

    /**
     * Bật/tắt phí xe theo lựa chọn của resident.
     * selected = có trong list → effectiveTo = null  (bật)
     * không selected           → effectiveTo = today (tắt)
     */
    private void updateParkingFees(Apartment apt, List<String> selectedFeeIds) {
        Set<String> selected = Set.copyOf(selectedFeeIds);
        List<Fees> parkingFees = feesRepository.findByApartmentAndType(apt, Fees.FeeType.PARKING);

        for (Fees fee : parkingFees) {
            boolean wantActive = selected.contains(fee.getId());
            boolean isActive   = fee.getEffectiveTo() == null;

            if (wantActive && !isActive) {
                fee.setEffectiveTo(null);
                feesRepository.save(fee);
                logger.info("Parking fee {} activated for apt {}", fee.getId(), apt.getId());
            } else if (!wantActive && isActive) {
                fee.setEffectiveTo(LocalDate.now());
                feesRepository.save(fee);
                logger.info("Parking fee {} deactivated for apt {}", fee.getId(), apt.getId());
            }
        }
    }

    /* ══════════════════════════════════════════════════════
       HELPERS
    ══════════════════════════════════════════════════════ */
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Phân loại xe từ tên fee để frontend hiển thị icon */
    private String detectVehicleType(String name) {
        if (name == null) return null;
        String lc = name.toLowerCase();
        if (lc.contains("ô tô") || lc.contains("o to") || lc.contains("car"))  return "car";
        if (lc.contains("đạp") || lc.contains("ebike") || lc.contains("xe điện")) return "ebike";
        if (lc.contains("máy") || lc.contains("motorbike") || lc.contains("motor")) return "motorbike";
        return null;
    }
}