package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.MomoIpnRequest;
import com.swp391.condocare_swp.entity.Invoice;
import com.swp391.condocare_swp.repository.InvoiceRepository;
import com.swp391.condocare_swp.service.MomoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller cho MoMo Payment
 * Base: /api/momo
 */
@RestController
@RequestMapping("/api/momo")
public class MomoController {

    private static final Logger logger = LoggerFactory.getLogger(MomoController.class);

    @Autowired private MomoService       momoService;
    @Autowired private InvoiceRepository invoiceRepo;

    /**
     * POST /api/momo/create-payment
     * Resident gọi khi click "Thanh toán MoMo"
     * Body: { "invoiceId": "INV2026030001" }
     *
     * Response: { payUrl, qrCodeUrl, deeplink, shortLink, orderId, ... }
     */
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, String> body) {
        String invoiceId = body.get("invoiceId");
        if (invoiceId == null || invoiceId.isBlank())
            return ResponseEntity.badRequest().body("Thiếu invoiceId");

        try {
            // Kiểm tra invoice tồn tại và chưa thanh toán
            Invoice invoice = invoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + invoiceId));

            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
                return ResponseEntity.badRequest().body("Hóa đơn này đã được thanh toán rồi!");

            // Kiểm tra resident chỉ có thể thanh toán hóa đơn của mình
            // (Optional security check — thêm nếu cần)
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("MoMo create payment — user={}, invoiceId={}", currentUser, invoiceId);

            Map<String, Object> result = momoService.createPayment(invoiceId, invoice.getTotalAmount());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error creating MoMo payment for invoice {}", invoiceId, e);
            return ResponseEntity.internalServerError().body("Lỗi tạo thanh toán MoMo: " + e.getMessage());
        }
    }

    /**
     * POST /api/momo/ipn
     * MoMo gọi về endpoint này sau khi user thanh toán xong
     * KHÔNG cần JWT — MoMo gọi trực tiếp, xác thực bằng HMAC signature
     *
     * ⚠️  Endpoint này phải được PERMIT_ALL trong Security config
     *     và phải accessible từ internet (dùng ngrok khi dev)
     */
    @PostMapping("/ipn")
    public ResponseEntity<?> handleIpn(@RequestBody MomoIpnRequest ipnRequest) {
        logger.info("MoMo IPN received: orderId={}, resultCode={}",
                ipnRequest.getOrderId(), ipnRequest.getResultCode());

        boolean valid = momoService.handleIpn(ipnRequest);

        if (!valid) {
            logger.warn("MoMo IPN signature invalid!");
            // MoMo yêu cầu trả về HTTP 204 dù có lỗi hay không
            return ResponseEntity.noContent().build();
        }

        // MoMo chỉ cần HTTP 204 No Content để xác nhận đã nhận IPN
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/momo/status/{invoiceId}
     * Frontend polling sau khi user quay về từ MoMo
     * Trả về: { invoiceId, status, paid, paidAt }
     */
    @GetMapping("/status/{invoiceId}")
    public ResponseEntity<?> checkStatus(@PathVariable String invoiceId) {
        try {
            return ResponseEntity.ok(momoService.checkPaymentStatus(invoiceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}