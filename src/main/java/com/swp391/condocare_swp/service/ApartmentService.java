package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ApartmentRepository;
import com.swp391.condocare_swp.repository.BuildingRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ApartmentService {

    private static final Logger logger = LoggerFactory.getLogger(ApartmentService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private ApartmentRepository apartmentRepo;
    @Autowired private BuildingRepository  buildingRepo;
    @Autowired private StaffRepository     staffRepo;

    // ─── BUILDING CRUD ────────────────────────────────────────────────────────

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
        String name         = body.get("name");
        String address      = body.get("address");
        String totalFloors  = body.get("totalFloors");
        String totalApts    = body.get("totalApartments");
        String managerId    = body.get("managerId");

        if (name == null || name.isBlank())    throw new RuntimeException("Tên tòa nhà không được để trống.");
        if (address == null || address.isBlank()) throw new RuntimeException("Địa chỉ không được để trống.");
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

    // ─── APARTMENT CRUD ───────────────────────────────────────────────────────

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

    public Map<String, Object> getApartmentDetail(String id) {
        Apartment apt = apartmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + id));
        return mapApartment(apt);
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

        if (apartmentRepo.findByNumberAndBuildingId(number, buildingId).isPresent())
            throw new RuntimeException("Căn hộ " + number + " đã tồn tại trong tòa nhà này.");

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

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private Map<String, Object> mapBuilding(Building b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               b.getId());
        m.put("name",             b.getName());
        m.put("address",          b.getAddress());
        m.put("totalFloors",      b.getTotalFloors());
        m.put("totalApartments",  b.getTotalApartments());
        m.put("managerName",      b.getManager() != null ? b.getManager().getFullName() : "—");
        m.put("managerId",        b.getManager() != null ? b.getManager().getId() : null);
        return m;
    }

    private Map<String, Object> mapApartment(Apartment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            a.getId());
        m.put("number",        a.getNumber());
        m.put("floor",         a.getFloor());
        m.put("area",          a.getArea());
        m.put("status",        a.getStatus().name());
        m.put("rentalStatus",  a.getRentalStatus().name());
        m.put("description",   a.getDescription());
        m.put("totalResidents", a.getTotalResident());
        m.put("totalVehicles",  a.getTotalVehicle());
        if (a.getBuilding() != null) {
            m.put("buildingId",   a.getBuilding().getId());
            m.put("buildingName", a.getBuilding().getName());
        }
        return m;
    }

    private synchronized String generateBuildingId() {
        for (int i = 1; i <= 999; i++) {
            String c = "BLD" + String.format("%03d", i);
            if (!buildingRepo.existsById(c)) return c;
        }
        return "BLD" + System.currentTimeMillis() % 1000000L;
    }

    private synchronized String generateApartmentId() {
        for (int i = 1; i <= 9999; i++) {
            String c = "APT" + String.format("%03d", i);
            if (!apartmentRepo.existsById(c)) return c;
        }
        return "APT" + System.currentTimeMillis() % 10000000L + idCounter.incrementAndGet();
    }
}