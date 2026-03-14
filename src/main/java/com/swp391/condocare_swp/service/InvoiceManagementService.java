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

@Service
public class InvoiceManagementService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceManagementService.class);

    @Autowired private InvoiceRepository      invoiceRepo;
    @Autowired private ApartmentRepository    apartmentRepo;
    @Autowired private FeesRepository         feesRepo;
    @Autowired private MeterReadingRepository meterRepo;
    @Autowired private StaffRepository        staffRepo;

    /* ══════════════════════════════════════════════════════
       THỐNG KÊ NHANH
    ══════════════════════════════════════════════════════ */
    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",   invoiceRepo.count());
        m.put("unpaid",  invoiceRepo.countByStatus(Invoice.InvoiceStatus.UNPAID));
        m.put("paid",    invoiceRepo.countByStatus(Invoice.InvoiceStatus.PAID));
        m.put("overdue", invoiceRepo.countByStatus(Invoice.InvoiceStatus.OVERDUE));
        return m;
    }

    /* ══════════════════════════════════════════════════════
       DANH SÁCH + TÌM KIẾM + LỌC
    ══════════════════════════════════════════════════════ */
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

        // Lọc status
        if (status != null && !status.isBlank() && !status.equals("ALL")) {
            Invoice.InvoiceStatus s = Invoice.InvoiceStatus.valueOf(status);
            all = all.stream().filter(i -> i.getStatus() == s).toList();
        }
        // Lọc tháng
        if (month != null) {
            all = all.stream().filter(i -> month.equals(i.getMonth())).toList();
        }
        // Lọc năm
        if (year != null) {
            all = all.stream().filter(i -> year.equals(i.getYear())).toList();
        }
        // Tìm kiếm keyword
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

        // Phân trang thủ công
        int total  = all.size();
        int offset = (int) pageable.getOffset();
        int end    = Math.min(offset + pageable.getPageSize(), total);
        List<Invoice> paged  = offset >= total ? List.of() : all.subList(offset, end);
        List<Map<String, Object>> content = paged.stream().map(this::mapToResponse).toList();
        return new PageImpl<>(content, pageable, total);
    }

    /* ══════════════════════════════════════════════════════
       CHI TIẾT
    ══════════════════════════════════════════════════════ */
    public Map<String, Object> getInvoiceDetail(String id) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + id));
        return mapToDetailResponse(inv);
    }

    /* ══════════════════════════════════════════════════════
       TẠO HÓA ĐƠN
    ══════════════════════════════════════════════════════ */
    @Transactional
    public Map<String, Object> createInvoice(InvoiceCreateRequest req) {
        Apartment apt = apartmentRepo.findById(req.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + req.getApartmentId()));

        // Kiểm tra trùng
        if (invoiceRepo.findByApartmentAndMonthAndYear(apt, req.getMonth(), req.getYear()).isPresent()) {
            throw new RuntimeException("Căn hộ " + apt.getNumber()
                    + " đã có hóa đơn tháng " + req.getMonth() + "/" + req.getYear());
        }

        // Chỉ số điện
        MeterReading electricReading = null;
        BigDecimal electricAmount = BigDecimal.ZERO;
        if (req.getElectricReadingId() != null && !req.getElectricReadingId().isBlank()) {
            electricReading = meterRepo.findById(req.getElectricReadingId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chỉ số điện"));
            electricAmount = electricReading.getTotalAmountDecimal();
        }

        // Chỉ số nước
        MeterReading waterReading = null;
        BigDecimal waterAmount = BigDecimal.ZERO;
        if (req.getWaterReadingId() != null && !req.getWaterReadingId().isBlank()) {
            waterReading = meterRepo.findById(req.getWaterReadingId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chỉ số nước"));
            waterAmount = waterReading.getTotalAmountDecimal();
        }

        // Phí dịch vụ
        Fees serviceFee = null;
        BigDecimal serviceAmount = BigDecimal.ZERO;
        if (req.getServiceFeeId() != null && !req.getServiceFeeId().isBlank()) {
            serviceFee = feesRepo.findById(req.getServiceFeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phí dịch vụ"));
            serviceAmount = nvl(serviceFee.getAmount());
        }

        // Phí xe
        Fees parkingFee = null;
        BigDecimal parkingAmount = BigDecimal.ZERO;
        if (req.getParkingFeeId() != null && !req.getParkingFeeId().isBlank()) {
            parkingFee = feesRepo.findById(req.getParkingFeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phí xe"));
            parkingAmount = nvl(parkingFee.getAmount());
        }

        BigDecimal total = electricAmount.add(waterAmount).add(serviceAmount).add(parkingAmount);

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff creator = staffRepo.findByUsername(currentUsername).orElse(null);

        // Due date: ngày 15 tháng sau
        int dueMonth = req.getMonth() == 12 ? 1 : req.getMonth() + 1;
        int dueYear  = req.getMonth() == 12 ? req.getYear() + 1 : req.getYear();

        Invoice invoice = new Invoice();
        invoice.setId(generateInvoiceId());
        invoice.setApartment(apt);
        invoice.setMonth(req.getMonth());
        invoice.setYear(req.getYear());
        invoice.setElectricReading(electricReading);
        invoice.setWaterReading(waterReading);
        invoice.setServiceFee(serviceFee);
        invoice.setParkingFee(parkingFee);
        invoice.setElectricAmount(electricAmount);
        invoice.setWaterAmount(waterAmount);
        invoice.setServiceAmount(serviceAmount);
        invoice.setParkingAmount(parkingAmount);
        invoice.setTotalAmount(total);
        invoice.setStatus(Invoice.InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDueDate(LocalDate.of(dueYear, dueMonth, 15));
        invoice.setCreatedBy(creator);

        invoiceRepo.save(invoice);
        logger.info("Created invoice {} for apt {} - {}/{} by {}",
                invoice.getId(), apt.getNumber(), req.getMonth(), req.getYear(), currentUsername);

        return mapToDetailResponse(invoice);
    }

    /* ══════════════════════════════════════════════════════
       CẬP NHẬT TRẠNG THÁI
    ══════════════════════════════════════════════════════ */
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

    /* ══════════════════════════════════════════════════════
       XÓA HÓA ĐƠN
    ══════════════════════════════════════════════════════ */
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

    /* ══════════════════════════════════════════════════════
       DROPDOWN DATA
    ══════════════════════════════════════════════════════ */
    public List<Map<String, Object>> getMeterReadings(String apartmentId, Integer month, Integer year) {
        Apartment apt = apartmentRepo.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ"));
        return meterRepo.findByApartmentAndMonthAndYear(apt, month, year).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",            r.getId());
            m.put("meterType",     r.getMeterType().name());
            m.put("previousIndex", r.getPreviousIndex());
            m.put("currentIndex",  r.getCurrentIndex());
            m.put("consumption",   r.getConsumption());
            m.put("unitPrice",     0); // unitPrice không có trong DB schema - tính từ total/consumption
            m.put("totalAmount",   r.getTotalAmount());
            return m;
        }).toList();
    }

    public List<Map<String, Object>> getActiveFees(String apartmentId) {
        Apartment apt = apartmentRepo.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ"));
        return feesRepo.findByApartmentAndEffectiveToIsNull(apt).stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",     f.getId());
            m.put("name",   f.getName());
            m.put("type",   f.getType().name());
            m.put("amount", f.getAmount());
            return m;
        }).toList();
    }

    /* ══════════════════════════════════════════════════════
       TẠO CHỈ SỐ ĐIỆN/NƯỚC (nhập tay từ form)
    ══════════════════════════════════════════════════════ */
    @Transactional
    public Map<String, Object> createMeterReading(Map<String, Object> body) {
        String apartmentId = (String) body.get("apartmentId");
        Integer month      = toInt(body.get("month"));
        Integer year       = toInt(body.get("year"));
        String typeStr     = (String) body.get("meterType");
        Double prevIdx     = toDouble(body.get("previousIndex"));
        Double currIdx     = toDouble(body.get("currentIndex"));
        Double unitPrice   = toDouble(body.get("unitPrice"));

        if (apartmentId == null || month == null || year == null || typeStr == null)
            throw new RuntimeException("Thiếu thông tin bắt buộc: apartmentId, month, year, meterType");
        if (currIdx <= prevIdx)
            throw new RuntimeException("Chỉ số mới phải lớn hơn chỉ số cũ");

        Apartment apt = apartmentRepo.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ: " + apartmentId));

        MeterReading.MeterType meterType = MeterReading.MeterType.valueOf(typeStr.toUpperCase());

        // Kiểm tra đã có chỉ số cho tháng này chưa
        meterRepo.findByApartmentAndMeterTypeAndMonthAndYear(apt, meterType, month, year)
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "Căn hộ " + apt.getNumber() + " đã có chỉ số " + typeStr +
                                    " tháng " + month + "/" + year + " (ID: " + existing.getId() + ")");
                });

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Staff recorder = staffRepo.findByUsername(currentUsername).orElse(null);

        double consumption = currIdx - prevIdx;
        double totalAmount = consumption * (unitPrice != null ? unitPrice : 0);

        MeterReading mr = new MeterReading();
        mr.setId(generateMeterReadingId());
        mr.setApartment(apt);
        mr.setMeterType(meterType);
        mr.setMonth(month);
        mr.setYear(year);
        mr.setPreviousIndex(BigDecimal.valueOf(prevIdx));
        mr.setCurrentIndex(BigDecimal.valueOf(currIdx));
        mr.setTotalAmount(totalAmount);
        mr.setRecordedBy(recorder);
        mr.setRecordedAt(LocalDateTime.now());

        meterRepo.save(mr);
        logger.info("Created MeterReading {} ({}) for apt {} - {}/{}", mr.getId(), typeStr, apartmentId, month, year);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",            mr.getId());
        result.put("meterType",     mr.getMeterType().name());
        result.put("previousIndex", mr.getPreviousIndex());
        result.put("currentIndex",  mr.getCurrentIndex());
        result.put("consumption",   mr.getConsumption());
        result.put("totalAmount",   mr.getTotalAmount());
        return result;
    }

    /* ══════════════════════════════════════════════════════
       PRIVATE HELPERS
    ══════════════════════════════════════════════════════ */
    private Map<String, Object> mapToResponse(Invoice i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              i.getId());
        m.put("apartmentId",     i.getApartment() != null ? i.getApartment().getId() : null);
        m.put("apartmentNumber", i.getApartment() != null ? i.getApartment().getNumber() : "—");
        m.put("buildingName",    (i.getApartment() != null && i.getApartment().getBuilding() != null)
                ? i.getApartment().getBuilding().getName() : "—");
        m.put("month",           i.getMonth());
        m.put("year",            i.getYear());
        m.put("electricAmount",  i.getElectricAmount());
        m.put("waterAmount",     i.getWaterAmount());
        m.put("serviceAmount",   i.getServiceAmount());
        m.put("parkingAmount",   i.getParkingAmount());
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
        if (i.getElectricReading() != null) {
            Map<String, Object> er = new LinkedHashMap<>();
            er.put("id",            i.getElectricReading().getId());
            er.put("previousIndex", i.getElectricReading().getPreviousIndex());
            er.put("currentIndex",  i.getElectricReading().getCurrentIndex());
            er.put("consumption",   i.getElectricReading().getConsumption());
            er.put("totalAmount",   i.getElectricReading().getTotalAmount());
            m.put("electricReading", er);
        }
        if (i.getWaterReading() != null) {
            Map<String, Object> wr = new LinkedHashMap<>();
            wr.put("id",            i.getWaterReading().getId());
            wr.put("previousIndex", i.getWaterReading().getPreviousIndex());
            wr.put("currentIndex",  i.getWaterReading().getCurrentIndex());
            wr.put("consumption",   i.getWaterReading().getConsumption());
            wr.put("totalAmount",   i.getWaterReading().getTotalAmount());
            m.put("waterReading", wr);
        }
        if (i.getServiceFee() != null) m.put("serviceFeeName", i.getServiceFee().getName());
        if (i.getParkingFee()  != null) m.put("parkingFeeName",  i.getParkingFee().getName());
        return m;
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Double d) return d;
        if (v instanceof Integer i) return i.doubleValue();
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }

    private synchronized String generateMeterReadingId() {
        String prefix = "MR" + String.format("%02d", LocalDate.now().getMonthValue())
                + LocalDate.now().getYear();
        for (int i = 1; i <= 99999; i++) {
            String id = prefix + String.format("%05d", i);
            if (!meterRepo.existsById(id)) return id;
        }
        return "MR" + System.currentTimeMillis();
    }

    private synchronized String generateInvoiceId() {
        String prefix = "INV" + LocalDate.now().getYear()
                + String.format("%02d", LocalDate.now().getMonthValue());
        for (int i = 1; i <= 9999; i++) {
            String id = prefix + String.format("%04d", i);
            if (!invoiceRepo.existsById(id)) return id;
        }
        return "INV" + System.currentTimeMillis();
    }
}