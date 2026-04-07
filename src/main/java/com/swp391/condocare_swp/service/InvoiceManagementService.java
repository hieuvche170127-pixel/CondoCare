package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.dto.InvoiceCreateRequest;
import com.swp391.condocare_swp.entity.*;
import com.swp391.condocare_swp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.swp391.condocare_swp.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InvoiceManagementService — Mô hình B, phí theo khung diện tích.
 *
 * FIX BUG 1: resolveApartment() hỗ trợ tra cứu bằng cả apartment ID ("APT001")
 *            lẫn apartment number ("A101") — giải quyết lỗi "Không tìm thấy căn hộ"
 *            khi frontend gửi số căn thay vì ID.
 *
 * Logic lọc FeeTemplate áp dụng cho 1 căn hộ:
 *   - PARKING / PER_APT / FIXED  : luôn áp dụng (min_area / max_area bỏ qua)
 *   - PER_M2 (phí quản lý)       : chỉ áp dụng khi diện tích căn hộ
 *                                  nằm trong [min_area, max_area]
 *                                  (NULL = không giới hạn phía đó)
 */
@Service
public class InvoiceManagementService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceManagementService.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    @Autowired private InvoiceRepository          invoiceRepo;
    @Autowired private InvoiceFeeDetailRepository detailRepo;
    @Autowired private ApartmentRepository        apartmentRepo;
    @Autowired private FeeTemplateRepository      feeTemplateRepo;
    @Autowired private VehicleRepository          vehicleRepo;
    @Autowired private StaffRepository            staffRepo;
    @Autowired private SecurityUtils              securityUtils; // [FIX] dùng chung SecurityUtils

    // ─── THỐNG KÊ ─────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",   invoiceRepo.count());
        m.put("unpaid",  invoiceRepo.countByStatus(Invoice.InvoiceStatus.UNPAID));
        m.put("paid",    invoiceRepo.countByStatus(Invoice.InvoiceStatus.PAID));
        m.put("overdue", invoiceRepo.countByStatus(Invoice.InvoiceStatus.OVERDUE));
        return m;
    }

    // ─── DANH SÁCH + LỌC ──────────────────────────────────────────────────────

    public Page<Map<String, Object>> listInvoices(
            String search, String status, String apartmentId,
            Integer month, Integer year, Pageable pageable) {

        List<Invoice> all;
        if (apartmentId != null && !apartmentId.isBlank()) {
            // FIX: dùng resolveApartment thay vì findById trực tiếp
            Apartment apt = resolveApartment(apartmentId);
            all = invoiceRepo.findByApartmentOrderByYearDescMonthDesc(apt);
        } else {
            all = invoiceRepo.findAllByOrderByYearDescMonthDesc();
        }

        if (status != null && !status.isBlank() && !status.equals("ALL")) {
            Invoice.InvoiceStatus s = Invoice.InvoiceStatus.valueOf(status.toUpperCase()); // [FIX]
            all = all.stream().filter(i -> i.getStatus() == s).toList();
        }
        if (month != null) all = all.stream().filter(i -> month.equals(i.getMonth())).toList();
        if (year  != null) all = all.stream().filter(i -> year.equals(i.getYear())).toList();
        if (search != null && !search.isBlank()) {
            String kw = search.toLowerCase();
            all = all.stream().filter(i -> {
                String aptNum  = i.getApartment() != null ? i.getApartment().getNumber().toLowerCase() : "";
                String bldName = (i.getApartment() != null && i.getApartment().getBuilding() != null)
                        ? i.getApartment().getBuilding().getName().toLowerCase() : "";
                return i.getId().toLowerCase().contains(kw) || aptNum.contains(kw) || bldName.contains(kw);
            }).toList();
        }

        int total  = all.size();
        int offset = (int) pageable.getOffset();
        int end    = Math.min(offset + pageable.getPageSize(), total);
        List<Invoice> paged = offset >= total ? List.of() : all.subList(offset, end);
        return new PageImpl<>(paged.stream().map(this::mapToResponse).toList(), pageable, total);
    }

    // ─── CHI TIẾT ─────────────────────────────────────────────────────────────

    public Map<String, Object> getInvoiceDetail(String id) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + id));
        return mapToDetailResponse(inv);
    }

    // ─── TẠO HÓA ĐƠN ─────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> createInvoice(InvoiceCreateRequest req) {
        // FIX: dùng resolveApartment — chấp nhận cả ID ("APT001") lẫn number ("A101")
        Apartment apt = resolveApartment(req.getApartmentId());

        if (invoiceRepo.findByApartmentAndMonthAndYear(apt, req.getMonth(), req.getYear()).isPresent())
            throw new RuntimeException("Căn hộ " + apt.getNumber()
                    + " đã có hóa đơn tháng " + req.getMonth() + "/" + req.getYear() + ".");

        String buildingId = apt.getBuilding().getId();
        Staff creator     = getCurrentStaff();

        List<FeeTemplate> activeTemplates =
                feeTemplateRepo.findByBuildingIdAndStatus(buildingId, FeeTemplate.FeeStatus.ACTIVE);

        List<InvoiceFeeDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (FeeTemplate ft : activeTemplates) {
            if (!isApplicableToArea(ft, apt.getArea())) continue;

            if (ft.getType() == FeeTemplate.FeeType.SERVICE) {
                BigDecimal qty    = calcServiceQty(ft, apt);
                BigDecimal amount = ft.getAmount().multiply(qty);
                details.add(buildDetail(ft, qty, amount));
                totalAmount = totalAmount.add(amount);

            } else if (ft.getType() == FeeTemplate.FeeType.PARKING) {
                long vehicleCount = countApprovedVehicles(apt.getId(), ft);
                if (vehicleCount > 0) {
                    BigDecimal qty    = BigDecimal.valueOf(vehicleCount);
                    BigDecimal amount = ft.getAmount().multiply(qty);
                    details.add(buildDetail(ft, qty, amount));
                    totalAmount = totalAmount.add(amount);
                }
            }
        }

        if (details.isEmpty())
            throw new RuntimeException(
                    "Không tìm thấy mẫu phí ACTIVE nào áp dụng được cho căn hộ này. " +
                            "Kiểm tra lại FeeTemplate (min_area/max_area).");

        int dueMonth = req.getMonth() == 12 ? 1 : req.getMonth() + 1;
        int dueYear  = req.getMonth() == 12 ? req.getYear() + 1 : req.getYear();

        Invoice invoice = new Invoice();
        invoice.setId(generateInvoiceId());
        invoice.setApartment(apt);
        invoice.setMonth(req.getMonth());
        invoice.setYear(req.getYear());
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(Invoice.InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDueDate(LocalDate.of(dueYear, dueMonth, 15));
        invoice.setCreatedBy(creator);
        invoiceRepo.save(invoice);

        for (InvoiceFeeDetail d : details) {
            d.setId(generateDetailId());
            d.setInvoice(invoice);
            detailRepo.save(d);
        }

        logger.info("Created invoice {} — apt {} ({}/{}) — total: {} — {} lines",
                invoice.getId(), apt.getNumber(), req.getMonth(), req.getYear(),
                totalAmount, details.size());

        return mapToDetailResponse(invoice);
    }

    // ─── PREVIEW ──────────────────────────────────────────────────────────────

    public Map<String, Object> previewInvoice(String apartmentId, Integer month, Integer year) {
        // FIX: dùng resolveApartment — chấp nhận cả ID ("APT001") lẫn number ("A101")
        Apartment apt = resolveApartment(apartmentId);

        boolean exists = invoiceRepo.findByApartmentAndMonthAndYear(apt, month, year).isPresent();

        String buildingId = apt.getBuilding().getId();
        List<FeeTemplate> activeTemplates =
                feeTemplateRepo.findByBuildingIdAndStatus(buildingId, FeeTemplate.FeeStatus.ACTIVE);

        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (FeeTemplate ft : activeTemplates) {
            if (!isApplicableToArea(ft, apt.getArea())) continue;

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("feeName",    ft.getName());
            line.put("feeType",    ft.getType().name());
            line.put("unitAmount", ft.getAmount());

            BigDecimal qty;
            if (ft.getType() == FeeTemplate.FeeType.SERVICE) {
                qty = calcServiceQty(ft, apt);
                line.put("qtyNote", ft.getUnit() == FeeTemplate.FeeUnit.PER_M2
                        ? apt.getArea() + " m²" : "1 căn");
            } else {
                long cnt = countApprovedVehicles(apt.getId(), ft);
                qty = BigDecimal.valueOf(cnt);
                line.put("qtyNote", cnt + " xe");
                if (cnt == 0) continue;
            }

            BigDecimal amount = ft.getAmount().multiply(qty);
            line.put("quantity", qty);
            line.put("amount",   amount);
            lines.add(line);
            total = total.add(amount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apartmentId",     apt.getId());     // luôn trả về ID thực (APT001)
        result.put("apartmentNumber", apt.getNumber()); // số căn để hiển thị (A101)
        result.put("area",            apt.getArea());
        result.put("month",           month);
        result.put("year",            year);
        result.put("alreadyExists",   exists);
        result.put("feeLines",        lines);
        result.put("totalAmount",     total);
        return result;
    }

    // ─── DANH SÁCH CĂN HỘ (dùng cho dropdown form tạo hóa đơn) ───────────────

    /**
     * Trả về danh sách tất cả căn hộ dạng gọn để populate dropdown.
     * GET /api/invoice-management/apartment-list
     */
    public List<Map<String, Object>> getApartmentList() {
        return apartmentRepo.findAllByOrderByBuilding_NameAscNumberAsc()
                .stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",           a.getId());
                    m.put("number",       a.getNumber());
                    m.put("buildingName", a.getBuilding() != null ? a.getBuilding().getName() : "");
                    m.put("area",         a.getArea());
                    m.put("status",       a.getStatus() != null ? a.getStatus().name() : "");
                    return m;
                })
                .toList();
    }

    // ─── CẬP NHẬT TRẠNG THÁI ──────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> updateStatus(String id, String newStatus) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + id));
        Invoice.InvoiceStatus s = Invoice.InvoiceStatus.valueOf(newStatus.toUpperCase()); // [FIX]
        invoice.setStatus(s);
        if (s == Invoice.InvoiceStatus.PAID) invoice.setPaidAt(LocalDateTime.now());
        invoiceRepo.save(invoice);
        logger.info("Invoice {} → {}", id, newStatus);
        return mapToResponse(invoice);
    }

    // ─── XÓA ──────────────────────────────────────────────────────────────────

    @Transactional
    public String deleteInvoice(String id) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + id));
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
            throw new RuntimeException("Không thể xóa hóa đơn đã thanh toán!");
        invoiceRepo.delete(invoice);
        logger.info("Deleted invoice: {}", id);
        return "Đã xóa hóa đơn thành công!";
    }

    // ─── FEE TEMPLATE (dropdown) ──────────────────────────────────────────────

    public List<Map<String, Object>> getActiveFeesForBuilding(String buildingId) {
        return feeTemplateRepo.findByBuildingIdAndStatus(buildingId, FeeTemplate.FeeStatus.ACTIVE)
                .stream().map(ft -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",      ft.getId());
                    m.put("name",    ft.getName());
                    m.put("type",    ft.getType().name());
                    m.put("amount",  ft.getAmount());
                    m.put("unit",    ft.getUnit().name());
                    m.put("minArea", ft.getMinArea());
                    m.put("maxArea", ft.getMaxArea());
                    return m;
                }).toList();
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    /**
     * FIX BUG 1 — Resolve apartment từ ID hoặc số căn.
     *
     * Ưu tiên:
     *   1. Thử findById(input) — input là ID như "APT001"
     *   2. Nếu không tìm thấy → thử findFirstByNumberIgnoreCase(input) — input là "A101"
     *   3. Nếu vẫn không thấy → ném RuntimeException rõ ràng
     *
     * Điều này giải quyết lỗi khi người dùng nhập số căn thay vì apartment ID
     * vào form tạo hóa đơn (placeholder "VD: A101" nhưng backend cần "APT001").
     */
    private Apartment resolveApartment(String apartmentIdOrNumber) {
        if (apartmentIdOrNumber == null || apartmentIdOrNumber.isBlank())
            throw new RuntimeException("Mã hoặc số căn hộ không được để trống.");

        // Thử tìm bằng ID trước (trường hợp dropdown đã chọn đúng ID)
        Optional<Apartment> byId = apartmentRepo.findById(apartmentIdOrNumber.trim());
        if (byId.isPresent()) return byId.get();

        // Fallback: tìm bằng apartment number (trường hợp user nhập "A101")
        Optional<Apartment> byNumber = apartmentRepo
                .findFirstByNumberIgnoreCase(apartmentIdOrNumber.trim());
        if (byNumber.isPresent()) {
            logger.debug("resolveApartment: '{}' resolved by number → id={}",
                    apartmentIdOrNumber, byNumber.get().getId());
            return byNumber.get();
        }

        throw new RuntimeException(
                "Không tìm thấy căn hộ: '" + apartmentIdOrNumber + "'. " +
                        "Vui lòng chọn từ danh sách hoặc nhập đúng mã căn hộ (VD: APT001) hoặc số căn (VD: A101).");
    }

    /**
     * Kiểm tra FeeTemplate có áp dụng được cho diện tích căn hộ không.
     *
     * Quy tắc:
     *  - Nếu unit KHÔNG phải PER_M2 (là PER_APT hoặc FIXED) → luôn áp dụng.
     *  - Nếu unit là PER_M2 → kiểm tra area nằm trong [min_area, max_area].
     *    NULL ở min_area  = không giới hạn dưới (≤ max_area là đủ).
     *    NULL ở max_area  = không giới hạn trên (≥ min_area là đủ).
     *    Cả hai NULL      = áp dụng cho mọi diện tích.
     */
    private boolean isApplicableToArea(FeeTemplate ft, BigDecimal area) {
        if (ft.getUnit() != FeeTemplate.FeeUnit.PER_M2) return true;
        if (area == null) return true;

        boolean aboveMin = ft.getMinArea() == null
                || area.compareTo(ft.getMinArea()) >= 0;
        boolean belowMax = ft.getMaxArea() == null
                || area.compareTo(ft.getMaxArea()) <= 0;

        return aboveMin && belowMax;
    }

    private BigDecimal calcServiceQty(FeeTemplate ft, Apartment apt) {
        if (ft.getUnit() == FeeTemplate.FeeUnit.PER_M2 && apt.getArea() != null)
            return apt.getArea();
        return BigDecimal.ONE;
    }

    /**
     * Đếm xe APPROVED + ACTIVE, khớp loại xe với tên FeeTemplate:
     *   "xe máy" / "motorbike"   → MOTORBIKE
     *   "ô tô"  / "car"          → CAR
     *   "điện"  / "electric"     → ELECTRIC_BIKE
     *   "xe đạp" / "bicycle"     → BICYCLE
     *   Còn lại                  → tất cả xe
     */
    private long countApprovedVehicles(String apartmentId, FeeTemplate ft) {
        List<Vehicle> vehicles = vehicleRepo.findByApartmentId(apartmentId)
                .stream()
                .filter(v -> v.getPendingStatus() == Vehicle.PendingStatus.APPROVED
                        && v.getStatus() == Vehicle.VehicleStatus.ACTIVE)
                .toList();

        String n = ft.getName().toLowerCase();
        if (n.contains("xe máy") || n.contains("motorbike"))
            return vehicles.stream().filter(v -> v.getType() == Vehicle.VehicleType.MOTORBIKE).count();
        if (n.contains("ô tô") || n.contains("car"))
            return vehicles.stream().filter(v -> v.getType() == Vehicle.VehicleType.CAR).count();
        if (n.contains("điện") || n.contains("electric"))
            return vehicles.stream().filter(v -> v.getType() == Vehicle.VehicleType.ELECTRIC_BIKE).count();
        if (n.contains("xe đạp") || n.contains("bicycle"))
            return vehicles.stream().filter(v -> v.getType() == Vehicle.VehicleType.BICYCLE).count();

        return vehicles.size();
    }

    private InvoiceFeeDetail buildDetail(FeeTemplate ft, BigDecimal qty, BigDecimal amount) {
        InvoiceFeeDetail d = new InvoiceFeeDetail();
        d.setFeeTemplate(ft);
        d.setFeeName(ft.getName());
        d.setFeeType(ft.getType());
        d.setUnitAmount(ft.getAmount());
        d.setQuantity(qty);
        d.setAmount(amount);
        return d;
    }

    private Map<String, Object> mapToResponse(Invoice i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              i.getId());
        m.put("apartmentId",     i.getApartment() != null ? i.getApartment().getId() : null);
        m.put("apartmentNumber", i.getApartment() != null ? i.getApartment().getNumber() : "—");
        m.put("buildingName",    (i.getApartment() != null && i.getApartment().getBuilding() != null)
                ? i.getApartment().getBuilding().getName() : "—");
        m.put("month",           i.getMonth());
        m.put("year",            i.getYear());
        m.put("totalAmount",     i.getTotalAmount());
        m.put("status",          i.getStatus() != null ? i.getStatus().name() : null);
        m.put("issuedAt",        i.getIssuedAt());
        m.put("dueDate",         i.getDueDate());
        m.put("paidAt",          i.getPaidAt());
        m.put("createdBy",       i.getCreatedBy() != null ? i.getCreatedBy().getFullName() : "—");
        return m;
    }

    private Map<String, Object> mapToDetailResponse(Invoice i) {
        Map<String, Object> m = mapToResponse(i);
        List<InvoiceFeeDetail> details = detailRepo.findByInvoiceId(i.getId());
        m.put("feeDetails", details.stream().map(d -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("id",         d.getId());
            line.put("feeName",    d.getFeeName());
            line.put("feeType",    d.getFeeType().name());
            line.put("unitAmount", d.getUnitAmount());
            line.put("quantity",   d.getQuantity());
            line.put("amount",     d.getAmount());
            return line;
        }).toList());
        return m;
    }

    // [FIX] Delegate sang SecurityUtils thay vì SecurityContextHolder inline
    private Staff getCurrentStaff() {
        return securityUtils.getCurrentStaff();
    }

    private synchronized String generateInvoiceId() {
        String prefix = "INV" + LocalDate.now().getYear()
                + String.format("%02d", LocalDate.now().getMonthValue());
        for (int i = 1; i <= 9999; i++) {
            String id = prefix + String.format("%04d", i);
            if (!invoiceRepo.existsById(id)) return id;
        }
        return "INV" + (System.currentTimeMillis() % 100000000000L);
    }

    private synchronized String generateDetailId() {
        for (int i = 1; i <= 99999; i++) {
            String id = "IFD" + String.format("%05d", i);
            if (!detailRepo.existsById(id)) return id;
        }
        return "IFD" + System.currentTimeMillis() % 100000L + idCounter.incrementAndGet();
    }
}