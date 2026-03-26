package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.ChangePasswordRequest;
import com.swp391.condocare_swp.dto.ProfileResponse;
import com.swp391.condocare_swp.dto.UpdateProfileRequest;
import com.swp391.condocare_swp.entity.AccessCard;
import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.FeeTemplate;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.entity.Vehicle;
import com.swp391.condocare_swp.repository.AccessCardRepository;
import com.swp391.condocare_swp.repository.FeeTemplateRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import com.swp391.condocare_swp.repository.VehicleRepository;
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
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired private StaffRepository       staffRepository;
    @Autowired private ResidentsRepository   residentsRepository;
    @Autowired private FeeTemplateRepository feeTemplateRepository;
    @Autowired private VehicleRepository     vehicleRepository;
    @Autowired private AccessCardRepository  accessCardRepository;
    @Autowired private PasswordEncoder       passwordEncoder;

    /* ══════════════════════════════════════════════════════════
       GET PROFILE
    ══════════════════════════════════════════════════════════ */
    public ProfileResponse getProfile() {
        String username = currentUsername();
        logger.info("Getting profile for user: {}", username);

        Staff staff = staffRepository.findByUsername(username).orElse(null);
        if (staff != null) return buildStaffProfile(staff);

        Residents resident = residentsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return buildResidentProfile(resident);
    }

    /* ══════════════════════════════════════════════════════════
       UPDATE PROFILE
    ══════════════════════════════════════════════════════════ */
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

    /* ══════════════════════════════════════════════════════════
       CHANGE PASSWORD
    ══════════════════════════════════════════════════════════ */
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

    /* ══════════════════════════════════════════════════════════
       PRIVATE — BUILD PROFILE RESPONSE
    ══════════════════════════════════════════════════════════ */

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
                .staffStatus(s.getStatus() != null ? s.getStatus().name() : null) // Staff.StaffStatus
                .roleName(s.getRole() != null ? s.getRole().getName() : null)
                .vehicles(new ArrayList<>())
                .feeTemplates(new ArrayList<>())
                .build();
    }

    private ProfileResponse buildResidentProfile(Residents r) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
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
                .residentStatus(r.getStatus() != null ? r.getStatus().name() : null); // Residents.ResidentStatus

        Apartment apt = r.getApartment();
        if (apt != null) {
            builder.apartmentId(apt.getId())
                    .apartmentNumber(apt.getNumber())
                    .apartmentArea(apt.getArea() != null ? apt.getArea().doubleValue() : null)
                    .buildingName(apt.getBuilding() != null ? apt.getBuilding().getName() : null);

            // VehicleRepository.findByResidentId(String) — Spring JPA tự resolve
            // vì Vehicle.resident là @ManyToOne, Spring map "residentId" → resident.id
            List<Vehicle> vehicles = vehicleRepository.findByResidentId(r.getId());
            builder.vehicles(vehicles.stream()
                    .map(this::toVehicleInfo)
                    .collect(Collectors.toList()));

            // FeeTemplate ACTIVE của tòa nhà
            if (apt.getBuilding() != null) {
                List<FeeTemplate> templates = feeTemplateRepository
                        .findByBuildingIdAndStatus(
                                apt.getBuilding().getId(),
                                FeeTemplate.FeeStatus.ACTIVE); // FeeTemplate.FeeStatus
                builder.feeTemplates(templates.stream()
                        .map(this::toFeeTemplateInfo)
                        .collect(Collectors.toList()));
            }
        } else {
            builder.vehicles(new ArrayList<>());
            builder.feeTemplates(new ArrayList<>());
        }

        // Thẻ từ — chỉ hiện khi ACTIVE
        // AccessCardRepository chỉ có findByResidentId(String) trả về List
        // → lọc CardStatus.ACTIVE thủ công (không dùng findFirstByResidentIdAndStatus)
        if (r.getStatus() == Residents.ResidentStatus.ACTIVE) {
            accessCardRepository.findByResidentId(r.getId())
                    .stream()
                    .filter(c -> c.getStatus() == AccessCard.CardStatus.ACTIVE) // AccessCard.CardStatus
                    .findFirst()
                    .ifPresent(card -> builder
                            .accessCardNumber(card.getCardNumber())
                            .accessCardStatus(card.getStatus().name()));
        }

        return builder.build();
    }

    /* ══════════════════════════════════════════════════════════
       PRIVATE — UPDATE PROFILE
    ══════════════════════════════════════════════════════════ */

    private String updateStaffProfile(Staff staff, UpdateProfileRequest req) {
        if (req.getEmail() != null
                && !req.getEmail().equals(staff.getEmail())
                && staffRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");

        staff.setFullName(req.getFullName());
        staff.setEmail(req.getEmail());
        staff.setPhone(req.getPhone());

        if (req.getDob() != null && !req.getDob().isBlank())
            staff.setDob(LocalDate.parse(req.getDob()));
        if (req.getGender() != null && !req.getGender().isBlank())
            staff.setGender(Staff.Gender.valueOf(req.getGender().toUpperCase())); // Staff.Gender

        staffRepository.save(staff);
        logger.info("Staff profile updated: {}", staff.getUsername());
        return "Cập nhật thông tin thành công!";
    }

    private String updateResidentProfile(Residents resident, UpdateProfileRequest req) {
        if (req.getEmail() != null
                && !req.getEmail().equals(resident.getEmail())
                && residentsRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");

        resident.setFullName(req.getFullName());
        resident.setEmail(req.getEmail());
        resident.setPhone(req.getPhone());

        if (req.getDob() != null && !req.getDob().isBlank())
            resident.setDob(LocalDate.parse(req.getDob()));
        if (req.getGender() != null && !req.getGender().isBlank())
            resident.setGender(Residents.Gender.valueOf(req.getGender().toUpperCase())); // Residents.Gender

        // idNumber: chỉ cho cập nhật khi còn PENDING
        // Sau khi ACTIVE → khóa để tránh giả mạo danh tính
        if (req.getIdNumber() != null) {
            if (resident.getStatus() == Residents.ResidentStatus.PENDING) {
                resident.setIdNumber(req.getIdNumber());
            } else {
                throw new RuntimeException(
                        "Không thể thay đổi CCCD sau khi tài khoản đã được xác minh");
            }
        }

        residentsRepository.save(resident);
        logger.info("Resident profile updated: {}", resident.getUsername());
        return "Cập nhật thông tin thành công!";
    }

    /* ══════════════════════════════════════════════════════════
       PRIVATE — MAPPERS
    ══════════════════════════════════════════════════════════ */

    private ProfileResponse.VehicleInfo toVehicleInfo(Vehicle v) {
        return ProfileResponse.VehicleInfo.builder()
                .id(v.getId())
                .type(v.getType()          != null ? v.getType().name()          : null) // Vehicle.VehicleType
                .licensePlate(v.getLicensePlate())
                .brand(v.getBrand())
                .model(v.getModel())
                .color(v.getColor())
                .durationType(v.getDurationType() != null ? v.getDurationType().name() : null)
                .registeredAt(v.getRegisteredAt() != null ? v.getRegisteredAt().toString() : null)
                .expiredAt(v.getExpiredAt()       != null ? v.getExpiredAt().toString()    : null)
                .pendingStatus(v.getPendingStatus() != null ? v.getPendingStatus().name()  : null)
                .status(v.getStatus()              != null ? v.getStatus().name()          : null) // Vehicle.VehicleStatus
                .rejectReason(v.getRejectReason())
                .build();
    }

    private ProfileResponse.FeeTemplateInfo toFeeTemplateInfo(FeeTemplate f) {
        return ProfileResponse.FeeTemplateInfo.builder()
                .id(f.getId())
                .name(f.getName())
                .type(f.getType() != null ? f.getType().name() : null)
                .unit(f.getUnit() != null ? f.getUnit().name() : null)
                .amount(f.getAmount() != null ? f.getAmount().doubleValue() : 0)
                .effectiveFrom(f.getEffectiveFrom() != null
                        ? f.getEffectiveFrom().toString() : null)
                .build();
    }

    /* ══════════════════════════════════════════════════════════
       HELPERS
    ══════════════════════════════════════════════════════════ */

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}