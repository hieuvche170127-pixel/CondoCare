package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.InvoiceCreateRequest;
import com.swp391.condocare_swp.service.InvoiceManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller quản lý hóa đơn (phía Staff)
 * Base: /api/invoice-management
 */
@RestController
@RequestMapping("/api/invoice-management")
public class InvoiceManagementController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceManagementController.class);
    @Autowired private InvoiceManagementService service;

    /** GET /api/invoice-management/stats */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * GET /api/invoice-management
     * Params: page, size, search, status, apartmentId, month, year, sort, direction
     */
    @GetMapping
    public ResponseEntity<?> listInvoices(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String apartmentId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "year") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sort));
            return ResponseEntity.ok(
                    service.listInvoices(search, status, apartmentId, month, year, pageable));
        } catch (Exception e) {
            logger.error("Error listing invoices", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /api/invoice-management/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getInvoiceDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** POST /api/invoice-management */
    @PostMapping
    public ResponseEntity<?> createInvoice(@Valid @RequestBody InvoiceCreateRequest request) {
        try {
            logger.info("POST /api/invoice-management - apt: {}, {}/{}",
                    request.getApartmentId(), request.getMonth(), request.getYear());
            return ResponseEntity.ok(service.createInvoice(request));
        } catch (Exception e) {
            logger.error("Error creating invoice", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PATCH /api/invoice-management/{id}/status
     * Body: { "status": "PAID" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null) return ResponseEntity.badRequest().body("Thiếu trường 'status'");
            return ResponseEntity.ok(service.updateStatus(id, newStatus));
        } catch (Exception e) {
            logger.error("Error updating invoice status {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** DELETE /api/invoice-management/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable String id) {
        try { return ResponseEntity.ok(service.deleteInvoice(id)); }
        catch (Exception e) {
            logger.error("Error deleting invoice {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/invoice-management/meter-readings?apartmentId=A01&month=3&year=2026
     * Lấy chỉ số điện/nước để điền vào form tạo hóa đơn
     */
    @GetMapping("/meter-readings")
    public ResponseEntity<?> getMeterReadings(
            @RequestParam String apartmentId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        try { return ResponseEntity.ok(service.getMeterReadings(apartmentId, month, year)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/invoice-management/meter-readings
     * Tạo mới chỉ số điện/nước khi staff nhập tay trong form tạo hóa đơn
     * Body: { apartmentId, month, year, meterType, previousIndex, currentIndex, unitPrice }
     */
    @PostMapping("/meter-readings")
    public ResponseEntity<?> createMeterReading(@RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.createMeterReading(body)); }
        catch (Exception e) {
            logger.error("Error creating meter reading", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/invoice-management/fees?apartmentId=A01
     * Lấy fees hiện hành của căn hộ để điền vào form
     */
    @GetMapping("/fees")
    public ResponseEntity<?> getActiveFees(@RequestParam String apartmentId) {
        try { return ResponseEntity.ok(service.getActiveFees(apartmentId)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}