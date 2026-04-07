package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.entity.FeeTemplate;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.BuildingRepository;
import com.swp391.condocare_swp.repository.FeeTemplateRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import com.swp391.condocare_swp.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ApartmentService {

    private static final Logger logger = LoggerFactory.getLogger(ApartmentService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    // ── Quyết định 33/2025/QĐ-UBND Hà Nội (hiệu lực 01/05/2025) ─────────────
    // Phí quản lý vận hành nhà chung cư CÓ thang máy: 1.200 – 16.500 đ/m²/tháng
    // Phí quản lý vận hành nhà chung cư KHÔNG thang máy: 700 – 5.000 đ/m²/tháng
    private static final BigDecimal FEE_ELEVATOR_MIN    = new BigDecimal("1200");
    private static final BigDecimal FEE_ELEVATOR_MAX    = new BigDecimal("16500");
    private static final BigDecimal FEE_ELEVATOR_DEFAULT = new BigDecimal("7000");
    private static final BigDecimal FEE_NO_ELEVATOR_MIN = new BigDecimal("700");
    private static final BigDecimal FEE_NO_ELEVATOR_MAX = new BigDecimal("5000");
    private static final BigDecimal FEE_NO_ELEVATOR_DEFAULT = new BigDecimal("2500");

    @Autowired private ApartmentRepository    apartmentRepo;
    @Autowired private BuildingRepository     buildingRepo;
    @Autowired private StaffRepository        staffRepo;
    @Autowired private SecurityUtils       securityUtils; // [FIX] dùng chung SecurityUtils
    @Autowired private FeeTemplateRepository  feeTemplateRepo;

    // ═════════════════════════════════════════════════════════════════════════
    // BUILDING CRUD
    // ═════════════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> getAllBuildings() {
        return buildingRepo.findAll().stream().map(this::mapBuilding).toList();
    }

    public Map<String, Object> getBuildingDetail(String id) {
        Building b = buildingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà: " + id));
        Map<String, Object> result = mapBuilding(b);
        result.put("apartments", apartmentRepo.findByBuildingId(id)
                .stream().map(this::mapApartment).toList());
        return result;
    }

    @Transactional
    public String createBuilding(Map<String, String> body) {
        String name        = body.get("name");
        String address     = body.get("address");
        String totalFloors = body.get("totalFloors");
        String totalApts   = body.get("totalApartments");
        String managerId   = body.get("managerId");

        if (name      == null || name.isBlank())    throw new RuntimeException("Tên tòa nhà không được để trống.");
        if (address   == null || address.isBlank()) throw new RuntimeException("Địa chỉ không được để trống.");
        if (managerId == null || managerId.isBlank()) throw new RuntimeException("Manager không được để trống.");

        Staff manager = staffRepo.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy staff: " + managerId));

        Building b = new Building();
        b.setId(generateBuildingId());
        b.setName(name.trim());
        b.setAddress(address.trim());
        b.setTotalFloors(totalFloors != null ? Integer.parseInt(totalFloors) : 0);
        b.setTotalApartments(totalApts != null ? Integer.parseInt(totalApts) : 0);
        b.setManager(manager);
        buildingRepo.save(b);
        logger.info("Created building: {}", b.getId());
        return "Tạo tòa nhà thành công!";
    }

    @Transactional
    public String updateBuilding(String id, Map<String, String> body) {
        Building b = buildingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà: " + id));
        if (body.containsKey("name")    && !body.get("name").isBlank())    b.setName(body.get("name").trim());
        if (body.containsKey("address") && !body.get("address").isBlank()) b.setAddress(body.get("address").trim());
        if (body.containsKey("totalFloors"))
            b.setTotalFloors(Integer.parseInt(body.get("totalFloors")));
        if (body.containsKey("totalApartments"))
            b.setTotalApartments(Integer.parseInt(body.get("totalApartments")));
        if (body.containsKey("managerId")) {
            Staff manager = staffRepo.findById(body.get("managerId"))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy staff: " + body.get("managerId")));
            b.setManager(manager);
        }
        buildingRepo.save(b);
        return "Cập nhật tòa nhà thành công!";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // APARTMENT CRUD
    // ═════════════════════════════════════════════════════════════════════════

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalBuildings",  buildingRepo.count());
        m.put("totalApartments", apartmentRepo.count());
        m.put("occupied",        apartmentRepo.countByStatus(Apartment.ApartmentStatus.OCCUPIED));
        m.put("empty",           apartmentRepo.countByStatus(Apartment.ApartmentStatus.EMPTY));
        m.put("maintenance",     apartmentRepo.countByStatus(Apartment.ApartmentStatus.MAINTENANCE));
        return m;
    }

    public List<Map<String, Object>> getAllApartments(String buildingId, String status) {
        Specification<Apartment> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (buildingId != null && !buildingId.isBlank())
                predicates.add(cb.equal(root.get("building").get("id"), buildingId));
            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"),
                        Apartment.ApartmentStatus.valueOf(status)));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return apartmentRepo.findAll(spec).stream().map(this::mapApartment).toList();
    }

    /**
     * Chi tiết căn hộ kèm danh sách phí dịch vụ áp dụng và ước tính chi phí
     * theo Quyết định 33/2025/QĐ-UBND UBND TP Hà Nội.
     */
    public Map<String, Object> getApartmentDetail(String id) {
        Apartment apt = apartmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + id));

        Map<String, Object> result = mapApartment(apt);

        // Lấy phí dịch vụ ACTIVE của tòa nhà
        List<FeeTemplate> fees = feeTemplateRepo.findByBuildingIdAndStatus(
                apt.getBuilding().getId(), FeeTemplate.FeeStatus.ACTIVE);

        BigDecimal totalEstimated = BigDecimal.ZERO;
        List<Map<String, Object>> feeDetails = new ArrayList<>();

        for (FeeTemplate ft : fees) {
            Map<String, Object> feeMap = mapFeeTemplate(ft);

            // Tính ước tính chi phí cho căn hộ này
            BigDecimal estimated;
            if (ft.getUnit() == FeeTemplate.FeeUnit.PER_M2) {
                estimated = ft.getAmount().multiply(apt.getArea());
            } else if (ft.getUnit() == FeeTemplate.FeeUnit.PER_APT) {
                estimated = ft.getAmount();
            } else { // FIXED
                estimated = ft.getAmount();
            }
            feeMap.put("estimatedAmount", estimated);
            totalEstimated = totalEstimated.add(estimated);
            feeDetails.add(feeMap);
        }

        result.put("activeFees", feeDetails);
        result.put("totalEstimatedMonthly", totalEstimated);
        result.put("feeNote",
                "Phí quản lý vận hành theo QĐ 33/2025/QĐ-UBND UBND TP Hà Nội (hiệu lực 01/05/2025). " +
                        "Chung cư có thang máy: 1.200–16.500 đ/m²/tháng. " +
                        "Chung cư không thang máy: 700–5.000 đ/m²/tháng.");

        return result;
    }

    @Transactional
    public String createApartment(Map<String, Object> body) {
        String buildingId = (String) body.get("buildingId");
        String number     = (String) body.get("number");
        String floorStr   = body.get("floor") != null ? body.get("floor").toString() : null;
        String areaStr    = body.get("area")  != null ? body.get("area").toString()  : null;

        if (buildingId == null || buildingId.isBlank()) throw new RuntimeException("BuildingId không được để trống.");
        if (number     == null || number.isBlank())     throw new RuntimeException("Số căn hộ không được để trống.");
        if (floorStr   == null)                         throw new RuntimeException("Tầng không được để trống.");
        if (areaStr    == null)                         throw new RuntimeException("Diện tích không được để trống.");

        Building building = buildingRepo.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà: " + buildingId));

        // Kiểm tra trùng lặp số căn hộ trong cùng tòa nhà
        if (apartmentRepo.findByNumberAndBuildingId(number.trim().toUpperCase(), buildingId).isPresent())
            throw new RuntimeException("Căn hộ " + number.toUpperCase() + " đã tồn tại trong tòa nhà này.");

        Apartment apt = new Apartment();
        apt.setId(generateApartmentId());
        apt.setNumber(number.trim().toUpperCase());
        apt.setFloor(Integer.parseInt(floorStr));
        apt.setArea(new BigDecimal(areaStr));
        apt.setBuilding(building);
        apt.setStatus(Apartment.ApartmentStatus.EMPTY);
        apt.setRentalStatus(Apartment.RentalStatus.AVAILABLE);
        apt.setDescription((String) body.get("description"));
        apt.setTotalResident(0);
        apt.setTotalVehicle(0);
        apartmentRepo.save(apt);
        logger.info("Created apartment {} in building {}", apt.getId(), buildingId);
        return "Tạo căn hộ thành công!";
    }

    @Transactional
    public String updateApartment(String id, Map<String, Object> body) {
        Apartment apt = apartmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + id));

        if (body.containsKey("floor"))       apt.setFloor(Integer.parseInt(body.get("floor").toString()));
        if (body.containsKey("area"))        apt.setArea(new BigDecimal(body.get("area").toString()));
        if (body.containsKey("description")) apt.setDescription((String) body.get("description"));
        if (body.containsKey("status"))
            apt.setStatus(Apartment.ApartmentStatus.valueOf((String) body.get("status")));
        if (body.containsKey("rentalStatus"))
            apt.setRentalStatus(Apartment.RentalStatus.valueOf((String) body.get("rentalStatus")));

        apartmentRepo.save(apt);
        logger.info("Updated apartment: {}", id);
        return "Cập nhật căn hộ thành công!";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FEE TEMPLATE CRUD
    // ═════════════════════════════════════════════════════════════════════════

    /** Lấy danh sách phí dịch vụ của tòa nhà */
    public List<Map<String, Object>> getFeeTemplates(String buildingId, String status) {
        List<FeeTemplate> list;
        if (status != null && !status.isBlank()) {
            list = feeTemplateRepo.findByBuildingIdAndStatus(
                    buildingId, FeeTemplate.FeeStatus.valueOf(status));
        } else {
            list = feeTemplateRepo.findByBuildingId(buildingId);
        }
        return list.stream().map(this::mapFeeTemplate).toList();
    }

    /** Tạo mẫu phí mới cho tòa nhà */
    @Transactional
    public Map<String, Object> createFeeTemplate(Map<String, Object> body) {
        String buildingId = (String) body.get("buildingId");
        String name       = (String) body.get("name");
        String type       = (String) body.get("type");
        String unit       = (String) body.get("unit");
        String amountStr  = body.get("amount") != null ? body.get("amount").toString() : null;

        if (buildingId == null || buildingId.isBlank()) throw new RuntimeException("BuildingId không được để trống.");
        if (name       == null || name.isBlank())       throw new RuntimeException("Tên phí không được để trống.");
        if (type       == null)                          throw new RuntimeException("Loại phí không được để trống.");
        if (unit       == null)                          throw new RuntimeException("Đơn vị tính không được để trống.");
        if (amountStr  == null)                          throw new RuntimeException("Đơn giá không được để trống.");

        BigDecimal amount = new BigDecimal(amountStr);
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Đơn giá phải lớn hơn 0.");

        // Kiểm tra phí PER_M2 theo khung giá QĐ 33/2025 Hà Nội
        if (FeeTemplate.FeeUnit.PER_M2.name().equals(unit) &&
                FeeTemplate.FeeType.SERVICE.name().equals(type)) {
            validateHanoiFeeRange(amount, body);
        }

        Building building = buildingRepo.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà: " + buildingId));

        Staff staff = getCurrentStaff();

        String effectiveFromStr = (String) body.get("effectiveFrom");
        String effectiveToStr   = (String) body.get("effectiveTo");

        FeeTemplate ft = new FeeTemplate();
        ft.setId(generateFeeTemplateId());
        ft.setBuilding(building);
        ft.setName(name.trim());
        ft.setType(FeeTemplate.FeeType.valueOf(type));
        ft.setUnit(FeeTemplate.FeeUnit.valueOf(unit));
        ft.setAmount(amount);
        ft.setEffectiveFrom(effectiveFromStr != null ? LocalDate.parse(effectiveFromStr) : LocalDate.now());
        ft.setEffectiveTo(effectiveToStr != null && !effectiveToStr.isBlank()
                ? LocalDate.parse(effectiveToStr) : null);
        ft.setStatus(FeeTemplate.FeeStatus.ACTIVE);
        ft.setCreatedBy(staff);

        // minArea / maxArea — chỉ có ý nghĩa với PER_M2, để NULL với các loại khác
        if (FeeTemplate.FeeUnit.PER_M2.name().equals(unit)) {
            String minAreaStr = body.get("minArea") != null ? body.get("minArea").toString() : null;
            String maxAreaStr = body.get("maxArea") != null ? body.get("maxArea").toString() : null;
            ft.setMinArea(minAreaStr != null && !minAreaStr.isBlank()
                    ? new BigDecimal(minAreaStr) : null);
            ft.setMaxArea(maxAreaStr != null && !maxAreaStr.isBlank()
                    ? new BigDecimal(maxAreaStr) : null);
        }

        feeTemplateRepo.save(ft);
        logger.info("Created fee template {} for building {}", ft.getId(), buildingId);
        return mapFeeTemplate(ft);
    }

    /** Cập nhật mẫu phí */
    @Transactional
    public Map<String, Object> updateFeeTemplate(String id, Map<String, Object> body) {
        FeeTemplate ft = feeTemplateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu phí: " + id));

        if (body.containsKey("name") && body.get("name") != null)
            ft.setName(body.get("name").toString().trim());

        if (body.containsKey("amount") && body.get("amount") != null) {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0)
                throw new RuntimeException("Đơn giá phải lớn hơn 0.");
            ft.setAmount(amount);
        }

        if (body.containsKey("unit") && body.get("unit") != null)
            ft.setUnit(FeeTemplate.FeeUnit.valueOf(body.get("unit").toString()));

        if (body.containsKey("type") && body.get("type") != null)
            ft.setType(FeeTemplate.FeeType.valueOf(body.get("type").toString()));

        if (body.containsKey("effectiveFrom") && body.get("effectiveFrom") != null)
            ft.setEffectiveFrom(LocalDate.parse(body.get("effectiveFrom").toString()));

        if (body.containsKey("effectiveTo")) {
            String eto = body.get("effectiveTo") != null ? body.get("effectiveTo").toString() : null;
            ft.setEffectiveTo(eto != null && !eto.isBlank() ? LocalDate.parse(eto) : null);
        }

        if (body.containsKey("status") && body.get("status") != null)
            ft.setStatus(FeeTemplate.FeeStatus.valueOf(body.get("status").toString()));

        // Cập nhật minArea / maxArea (chỉ áp dụng khi unit là PER_M2)
        if (ft.getUnit() == FeeTemplate.FeeUnit.PER_M2) {
            if (body.containsKey("minArea")) {
                String v = body.get("minArea") != null ? body.get("minArea").toString() : null;
                ft.setMinArea(v != null && !v.isBlank() ? new BigDecimal(v) : null);
            }
            if (body.containsKey("maxArea")) {
                String v = body.get("maxArea") != null ? body.get("maxArea").toString() : null;
                ft.setMaxArea(v != null && !v.isBlank() ? new BigDecimal(v) : null);
            }
        } else {
            // Không phải PER_M2 → clear về null
            ft.setMinArea(null);
            ft.setMaxArea(null);
        }

        feeTemplateRepo.save(ft);
        logger.info("Updated fee template: {}", id);
        return mapFeeTemplate(ft);
    }

    /** Vô hiệu hoá mẫu phí (soft delete) */
    @Transactional
    public String deactivateFeeTemplate(String id) {
        FeeTemplate ft = feeTemplateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu phí: " + id));
        ft.setStatus(FeeTemplate.FeeStatus.INACTIVE);
        feeTemplateRepo.save(ft);
        return "Đã vô hiệu hoá mẫu phí.";
    }

    /**
     * Trả về bộ mẫu phí gợi ý theo Quyết định 33/2025/QĐ-UBND UBND TP Hà Nội.
     * Frontend sẽ dùng để hiển thị quick-add suggestions.
     */
    public Map<String, Object> getHanoiDecreeSuggestions() {
        Map<String, Object> decree = new LinkedHashMap<>();
        decree.put("decree",    "Quyết định 33/2025/QĐ-UBND UBND TP Hà Nội");
        decree.put("effectiveDate", "2025-05-01");
        decree.put("note",      "Khung giá chưa bao gồm dịch vụ cao cấp: bể bơi, xông hơi, truyền hình cáp, internet.");

        // Khung phí quản lý vận hành
        Map<String, Object> serviceFeesWithElevator = new LinkedHashMap<>();
        serviceFeesWithElevator.put("label",       "Phí QLVH – chung cư CÓ thang máy (PER_M2)");
        serviceFeesWithElevator.put("type",        "SERVICE");
        serviceFeesWithElevator.put("unit",        "PER_M2");
        serviceFeesWithElevator.put("minAmount",   FEE_ELEVATOR_MIN);
        serviceFeesWithElevator.put("maxAmount",   FEE_ELEVATOR_MAX);
        serviceFeesWithElevator.put("defaultAmount", FEE_ELEVATOR_DEFAULT);
        serviceFeesWithElevator.put("description", "Áp dụng cho căn hộ có thang máy, tính theo m²/tháng");

        Map<String, Object> serviceFeesNoElevator = new LinkedHashMap<>();
        serviceFeesNoElevator.put("label",       "Phí QLVH – chung cư KHÔNG thang máy (PER_M2)");
        serviceFeesNoElevator.put("type",        "SERVICE");
        serviceFeesNoElevator.put("unit",        "PER_M2");
        serviceFeesNoElevator.put("minAmount",   FEE_NO_ELEVATOR_MIN);
        serviceFeesNoElevator.put("maxAmount",   FEE_NO_ELEVATOR_MAX);
        serviceFeesNoElevator.put("defaultAmount", FEE_NO_ELEVATOR_DEFAULT);
        serviceFeesNoElevator.put("description", "Áp dụng cho căn hộ không có thang máy, tính theo m²/tháng");

        // Phí gửi xe (mức phổ biến thực tế tại Hà Nội)
        List<Map<String, Object>> parkingFees = new ArrayList<>();

        parkingFees.add(Map.of(
                "label",         "Phí gửi xe máy",
                "type",          "PARKING",
                "unit",          "FIXED",
                "defaultAmount", new BigDecimal("100000"),
                "description",   "Xe máy/xe điện – 100.000 đ/xe/tháng"
        ));
        parkingFees.add(Map.of(
                "label",         "Phí gửi ô tô",
                "type",          "PARKING",
                "unit",          "FIXED",
                "defaultAmount", new BigDecimal("1200000"),
                "description",   "Ô tô – 1.200.000 đ/xe/tháng"
        ));
        parkingFees.add(Map.of(
                "label",         "Phí gửi xe đạp",
                "type",          "PARKING",
                "unit",          "FIXED",
                "defaultAmount", new BigDecimal("50000"),
                "description",   "Xe đạp – 50.000 đ/xe/tháng"
        ));

        decree.put("serviceFeesWithElevator",    serviceFeesWithElevator);
        decree.put("serviceFeesNoElevator",      serviceFeesNoElevator);
        decree.put("parkingFeeSuggestions",      parkingFees);

        return decree;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Kiểm tra mức phí PER_M2 SERVICE có nằm trong khung QĐ 33/2025 không.
     * Chỉ cảnh báo (không block) – để BQL linh hoạt áp mức riêng nếu đã có thỏa thuận hội nghị.
     */
    private void validateHanoiFeeRange(BigDecimal amount, Map<String, Object> body) {
        // Xác định có thang máy hay không qua flag "hasElevator" trong body (true/false)
        Object elevatorFlag = body.get("hasElevator");
        boolean hasElevator = elevatorFlag == null || Boolean.parseBoolean(elevatorFlag.toString());

        BigDecimal min = hasElevator ? FEE_ELEVATOR_MIN    : FEE_NO_ELEVATOR_MIN;
        BigDecimal max = hasElevator ? FEE_ELEVATOR_MAX    : FEE_NO_ELEVATOR_MAX;

        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            String type = hasElevator ? "có thang máy" : "không thang máy";
            logger.warn("Phí {}/m² nằm ngoài khung QĐ 33/2025 cho chung cư {}: [{} – {}]",
                    amount, type, min, max);
            // Không throw – chỉ log để admin biết, vì hội nghị nhà chung cư có thể đã thỏa thuận mức riêng
        }
    }

    // [FIX] Delegate sang SecurityUtils thay vì SecurityContextHolder inline
    private Staff getCurrentStaff() {
        return securityUtils.getCurrentStaff();
    }

    private Map<String, Object> mapBuilding(Building b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              b.getId());
        m.put("name",            b.getName());
        m.put("address",         b.getAddress());
        m.put("totalFloors",     b.getTotalFloors());
        m.put("totalApartments", b.getTotalApartments());
        m.put("managerName",     b.getManager() != null ? b.getManager().getFullName() : "—");
        m.put("managerId",       b.getManager() != null ? b.getManager().getId() : null);
        return m;
    }

    private Map<String, Object> mapApartment(Apartment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             a.getId());
        m.put("number",         a.getNumber());
        m.put("floor",          a.getFloor());
        m.put("area",           a.getArea());
        m.put("status",         a.getStatus().name());
        m.put("rentalStatus",   a.getRentalStatus().name());
        m.put("description",    a.getDescription());
        m.put("totalResidents", a.getTotalResident());
        m.put("totalVehicles",  a.getTotalVehicle());
        if (a.getBuilding() != null) {
            m.put("buildingId",   a.getBuilding().getId());
            m.put("buildingName", a.getBuilding().getName());
        }
        return m;
    }

    private Map<String, Object> mapFeeTemplate(FeeTemplate ft) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            ft.getId());
        m.put("name",          ft.getName());
        m.put("type",          ft.getType().name());
        m.put("typeLabel",     ft.getType() == FeeTemplate.FeeType.SERVICE ? "Dịch vụ" : "Gửi xe");
        m.put("unit",          ft.getUnit().name());
        m.put("unitLabel",     switch (ft.getUnit()) {
            case PER_M2  -> "đ/m²/tháng";
            case PER_APT -> "đ/căn/tháng";
            case FIXED   -> "đ/lần (cố định)";
        });
        m.put("amount",        ft.getAmount());
        m.put("effectiveFrom", ft.getEffectiveFrom());
        m.put("effectiveTo",   ft.getEffectiveTo());
        m.put("status",        ft.getStatus().name());
        m.put("statusLabel",   ft.getStatus() == FeeTemplate.FeeStatus.ACTIVE ? "Đang áp dụng" : "Không hoạt động");
        m.put("minArea",       ft.getMinArea());
        m.put("maxArea",       ft.getMaxArea());
        // Mô tả khung diện tích cho UI
        if (ft.getUnit() == FeeTemplate.FeeUnit.PER_M2
                && (ft.getMinArea() != null || ft.getMaxArea() != null)) {
            String areaRange = (ft.getMinArea() != null ? ft.getMinArea() + " m²" : "0 m²")
                    + " – " + (ft.getMaxArea() != null ? ft.getMaxArea() + " m²" : "không giới hạn");
            m.put("areaRange", areaRange);
        } else {
            m.put("areaRange", null);
        }
        if (ft.getBuilding() != null) {
            m.put("buildingId",   ft.getBuilding().getId());
            m.put("buildingName", ft.getBuilding().getName());
        }
        if (ft.getCreatedBy() != null) {
            m.put("createdBy", ft.getCreatedBy().getFullName());
        }
        m.put("createdAt", ft.getCreatedAt());
        return m;
    }

    // ─── ID Generators ────────────────────────────────────────────────────────

    private synchronized String generateBuildingId() {
        for (int i = 1; i <= 999; i++) {
            String c = "BLD" + String.format("%03d", i);
            if (!buildingRepo.existsById(c)) return c;
        }
        return "BLD" + System.currentTimeMillis() % 1000000L;
    }

    private synchronized String generateApartmentId() {
        for (int i = 1; i <= 9999; i++) {
            String c = "APT" + String.format("%04d", i);
            if (!apartmentRepo.existsById(c)) return c;
        }
        return "APT" + System.currentTimeMillis() % 10000000L + idCounter.incrementAndGet();
    }

    private synchronized String generateFeeTemplateId() {
        for (int i = 1; i <= 9999; i++) {
            String c = "FEE" + String.format("%04d", i);
            if (!feeTemplateRepo.existsById(c)) return c;
        }
        return "FEE" + System.currentTimeMillis() % 10000000L;
    }
}