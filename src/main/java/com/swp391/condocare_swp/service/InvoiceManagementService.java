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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InvoiceManagementService — Mô hình B
 * Hóa đơn chỉ gồm: Phí quản lý (SERVICE) + Phí gửi xe (PARKING).
 * Điện / nước do EVN thu trực tiếp — KHÔNG quản lý ở đây.
 *
 * Luồng tạo hóa đơn:
 *   1. Staff chọn căn hộ + tháng/năm
 *   2. Hệ thống tự động tính phí dựa trên FeeTemplate ACTIVE của tòa nhà
 *      và số xe APPROVED của căn hộ đó trong tháng
 *   3. Snapshot từng dòng phí vào invoice_fee_detail
 *   4. Tổng tiền = tổng các dòng detail
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
            Apartment apt = apartmentRepo.findById(apartmentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + apartmentId));
            all = invoiceRepo.findByApartmentOrderByYearDescMonthDesc(apt);
        } else {
            all = invoiceRepo.findAllByOrderByYearDescMonthDesc();
        }

        if (status != null && !status.isBlank() && !status.equals("ALL")) {
            Invoice.InvoiceStatus s = Invoice.InvoiceStatus.valueOf(status);
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
                return i.getId().toLowerCase().contains(kw)
                        || aptNum.contains(kw)
                        || bldName.contains(kw);
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

    // ─── TẠO HÓA ĐƠN TỰ ĐỘNG ─────────────────────────────────────────────────

    /**
     * Tạo hóa đơn cho 1 căn hộ theo tháng/năm.
     * Tự động tính:
     *   - Phí quản lý = FeeTemplate (SERVICE, PER_M2) × diện tích căn hộ
     *   - Phí gửi xe  = FeeTemplate (PARKING, FIXED) × số xe APPROVED của căn hộ (từng loại)
     */
    @Transactional
    public Map<String, Object> createInvoice(InvoiceCreateRequest req) {
        Apartment apt = apartmentRepo.findById(req.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + req.getApartmentId()));

        if (invoiceRepo.findByApartmentAndMonthAndYear(apt, req.getMonth(), req.getYear()).isPresent())
            throw new RuntimeException("Căn hộ " + apt.getNumber()
                    + " đã có hóa đơn tháng " + req.getMonth() + "/" + req.getYear() + ".");

        String buildingId = apt.getBuilding().getId();
        Staff creator     = getCurrentStaff();

        // Lấy tất cả FeeTemplate ACTIVE của tòa nhà
        List<FeeTemplate> activeTemplates =
                feeTemplateRepo.findByBuildingIdAndStatus(buildingId, FeeTemplate.FeeStatus.ACTIVE);

        List<InvoiceFeeDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (FeeTemplate ft : activeTemplates) {
            if (ft.getType() == FeeTemplate.FeeType.SERVICE) {
                // ── Phí quản lý ───────────────────────────────────────────────
                BigDecimal qty    = calcServiceQty(ft, apt);
                BigDecimal amount = ft.getAmount().multiply(qty);
                details.add(buildDetail(ft, qty, amount));
                totalAmount = totalAmount.add(amount);

            } else if (ft.getType() == FeeTemplate.FeeType.PARKING) {
                // ── Phí gửi xe: đếm số xe APPROVED khớp loại ─────────────────
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
                    "Không tìm thấy mẫu phí ACTIVE nào cho tòa nhà này. " +
                            "Vui lòng cấu hình FeeTemplate trước.");

        // Xác định ngày đến hạn = ngày 15 tháng sau
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

        // Lưu từng dòng detail với FK → invoice vừa tạo
        for (InvoiceFeeDetail d : details) {
            d.setId(generateDetailId());
            d.setInvoice(invoice);
            detailRepo.save(d);
        }

        logger.info("Created invoice {} for apt {} ({}/{}) — total: {} — {} fee lines",
                invoice.getId(), apt.getNumber(), req.getMonth(), req.getYear(),
                totalAmount, details.size());

        return mapToDetailResponse(invoice);
    }

    // ─── XEM PHÍ DỰ TÍNH (preview trước khi tạo) ─────────────────────────────

    /**
     * GET /api/invoice-management/preview?apartmentId=&month=&year=
     * Trả về chi tiết phí dự tính — dùng để Staff xem trước trước khi bấm "Tạo hóa đơn".
     */
    public Map<String, Object> previewInvoice(String apartmentId, Integer month, Integer year) {
        Apartment apt = apartmentRepo.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + apartmentId));

        boolean exists = invoiceRepo.findByApartmentAndMonthAndYear(apt, month, year).isPresent();

        String buildingId = apt.getBuilding().getId();
        List<FeeTemplate> activeTemplates =
                feeTemplateRepo.findByBuildingIdAndStatus(buildingId, FeeTemplate.FeeStatus.ACTIVE);

        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (FeeTemplate ft : activeTemplates) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("feeName",   ft.getName());
            line.put("feeType",   ft.getType().name());
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
            }

            BigDecimal amount = ft.getAmount().multiply(qty);
            line.put("quantity", qty);
            line.put("amount",   amount);
            lines.add(line);
            total = total.add(amount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apartmentId",     apt.getId());
        result.put("apartmentNumber", apt.getNumber());
        result.put("month",           month);
        result.put("year",            year);
        result.put("alreadyExists",   exists);
        result.put("feeLines",        lines);
        result.put("totalAmount",     total);
        return result;
    }

    // ─── CẬP NHẬT TRẠNG THÁI ──────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> updateStatus(String id, String newStatus) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + id));
        Invoice.InvoiceStatus s = Invoice.InvoiceStatus.valueOf(newStatus);
        invoice.setStatus(s);
        if (s == Invoice.InvoiceStatus.PAID) invoice.setPaidAt(LocalDateTime.now());
        invoiceRepo.save(invoice);
        logger.info("Invoice {} → {}", id, newStatus);
        return mapToResponse(invoice);
    }

    // ─── XÓA HÓA ĐƠN ─────────────────────────────────────────────────────────

    @Transactional
    public String deleteInvoice(String id) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + id));
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
            throw new RuntimeException("Không thể xóa hóa đơn đã thanh toán!");
        invoiceRepo.delete(invoice); // cascade xóa fee_details
        logger.info("Deleted invoice: {}", id);
        return "Đã xóa hóa đơn thành công!";
    }

    // ─── LẤY DANH SÁCH FEE TEMPLATE (dùng cho dropdown UI) ───────────────────

    public List<Map<String, Object>> getActiveFeesForBuilding(String buildingId) {
        return feeTemplateRepo.findByBuildingIdAndStatus(buildingId, FeeTemplate.FeeStatus.ACTIVE)
                .stream().map(ft -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",     ft.getId());
                    m.put("name",   ft.getName());
                    m.put("type",   ft.getType().name());
                    m.put("amount", ft.getAmount());
                    m.put("unit",   ft.getUnit().name());
                    return m;
                }).toList();
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    /**
     * Tính quantity cho phí SERVICE:
     *   PER_M2 → diện tích căn hộ
     *   PER_APT / FIXED → 1
     */
    private BigDecimal calcServiceQty(FeeTemplate ft, Apartment apt) {
        if (ft.getUnit() == FeeTemplate.FeeUnit.PER_M2 && apt.getArea() != null)
            return apt.getArea();
        return BigDecimal.ONE;
    }

    /**
     * Đếm số xe APPROVED + ACTIVE của căn hộ.
     * Logic khớp loại xe với FeeTemplate:
     *   FeeTemplate name chứa "xe máy" / "motorbike" → MOTORBIKE
     *   FeeTemplate name chứa "ô tô" / "car"         → CAR
     *   FeeTemplate name chứa "điện" / "electric"    → ELECTRIC_BIKE
     *   Còn lại (PARKING chung)                       → tất cả xe
     */
    private long countApprovedVehicles(String apartmentId, FeeTemplate ft) {
        List<Vehicle> vehicles = vehicleRepo.findByApartmentId(apartmentId)
                .stream()
                .filter(v -> v.getPendingStatus() == Vehicle.PendingStatus.APPROVED
                        && v.getStatus() == Vehicle.VehicleStatus.ACTIVE)
                .toList();

        String nameLower = ft.getName().toLowerCase();
        if (nameLower.contains("xe máy") || nameLower.contains("motorbike"))
            return vehicles.stream().filter(v -> v.getType() == Vehicle.VehicleType.MOTORBIKE).count();
        if (nameLower.contains("ô tô") || nameLower.contains("car"))
            return vehicles.stream().filter(v -> v.getType() == Vehicle.VehicleType.CAR).count();
        if (nameLower.contains("điện") || nameLower.contains("electric"))
            return vehicles.stream().filter(v -> v.getType() == Vehicle.VehicleType.ELECTRIC_BIKE).count();

        // Parking chung → tất cả xe
        return vehicles.size();
    }

    private InvoiceFeeDetail buildDetail(FeeTemplate ft, BigDecimal qty, BigDecimal amount) {
        InvoiceFeeDetail d = new InvoiceFeeDetail();
        d.setFeeTemplate(ft);
        d.setFeeName(ft.getName());        // snapshot
        d.setFeeType(ft.getType());
        d.setUnitAmount(ft.getAmount());   // snapshot
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

        // Chi tiết từng dòng phí
        List<InvoiceFeeDetail> details = detailRepo.findByInvoiceId(i.getId());
        List<Map<String, Object>> feeLines = details.stream().map(d -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("id",         d.getId());
            line.put("feeName",    d.getFeeName());
            line.put("feeType",    d.getFeeType().name());
            line.put("unitAmount", d.getUnitAmount());
            line.put("quantity",   d.getQuantity());
            line.put("amount",     d.getAmount());
            return line;
        }).toList();

        m.put("feeDetails", feeLines);
        return m;
    }

    private Staff getCurrentStaff() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return staffRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Staff đang đăng nhập."));
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