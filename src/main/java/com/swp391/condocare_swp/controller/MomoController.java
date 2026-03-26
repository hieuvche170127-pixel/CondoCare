package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.MomoIpnRequest;
import com.swp391.condocare_swp.entity.Invoice;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.repository.InvoiceRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
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
 *
 * Thay đổi so với phiên bản cũ:
 * - createPayment() lấy residentId từ JWT (SecurityContext) và truyền vào MomoService.
 * - Bổ sung @GetMapping("/status/{invoiceId}") đã bị thiếu annotation trong file cũ.
 */
@RestController
@RequestMapping("/api/momo")
public class MomoController {

    private static final Logger logger = LoggerFactory.getLogger(MomoController.class);

    @Autowired private MomoService         momoService;
    @Autowired private InvoiceRepository   invoiceRepo;
    @Autowired private ResidentsRepository residentsRepo;

    /**
     * POST /api/momo/create-payment
     * Resident gọi khi click "Thanh toán MoMo".
     * Body: { "invoiceId": "INV2026030001" }
     */
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, String> body) {
        String invoiceId = body.get("invoiceId");
        if (invoiceId == null || invoiceId.isBlank())
            return ResponseEntity.badRequest().body("Thiếu invoiceId");

        try {
            Invoice invoice = invoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + invoiceId));

            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
                return ResponseEntity.badRequest().body("Hóa đơn này đã được thanh toán rồi!");

            // Lấy residentId từ JWT để truyền vào extraData của MoMo request.
            // IPN callback sẽ đọc extraData để gán paid_by — không cần tìm ngược từ apartment.
            String username   = SecurityContextHolder.getContext().getAuthentication().getName();
            String residentId = residentsRepo.findByUsername(username)
                    .map(Residents::getId)
                    .orElse(null);

            logger.info("MoMo create payment — user={}, residentId={}, invoiceId={}",
                    username, residentId, invoiceId);

            Map<String, Object> result =
                    momoService.createPayment(invoiceId, invoice.getTotalAmount(), residentId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error creating MoMo payment for invoice {}", invoiceId, e);
            return ResponseEntity.internalServerError()
                    .body("Lỗi tạo thanh toán MoMo: " + e.getMessage());
        }
    }

    /**
     * POST /api/momo/ipn
     * MoMo gọi về endpoint này sau khi user thanh toán xong.
     * KHÔNG cần JWT — xác thực bằng HMAC signature.
     * Phải được PERMIT_ALL trong SecurityConfig.
     */
    @PostMapping("/ipn")
    public ResponseEntity<?> handleIpn(@RequestBody MomoIpnRequest ipnRequest) {
        logger.info("MoMo IPN received: orderId={}, resultCode={}",
                ipnRequest.getOrderId(), ipnRequest.getResultCode());

        boolean valid = momoService.handleIpn(ipnRequest);

        if (!valid) {
            logger.warn("MoMo IPN signature invalid!");
        }
        // MoMo yêu cầu HTTP 204 No Content dù hợp lệ hay không
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/momo/return?orderId=...&resultCode=0&...
     * MoMo redirect user về URL này sau khi thanh toán xong.
     * Dùng khi dev local không có ngrok.
     */
    @GetMapping("/return")
    public ResponseEntity<?> handleReturn(
            @RequestParam String orderId,
            @RequestParam int resultCode,
            @RequestParam String signature,
            @RequestParam(required = false, defaultValue = "") String extraData,
            @RequestParam(required = false, defaultValue = "") String message,
            @RequestParam(required = false, defaultValue = "0") long amount,
            @RequestParam(required = false, defaultValue = "0") long transId,
            @RequestParam(required = false, defaultValue = "") String orderInfo,
            @RequestParam(required = false, defaultValue = "") String orderType,
            @RequestParam(required = false, defaultValue = "") String payType,
            @RequestParam(required = false, defaultValue = "0") long responseTime,
            @RequestParam(required = false, defaultValue = "") String requestId) {

        logger.info("MoMo RETURN — orderId={}, resultCode={}", orderId, resultCode);

        if (resultCode == 0) {
            MomoIpnRequest ipn = new MomoIpnRequest();
            ipn.setOrderId(orderId);
            ipn.setResultCode(resultCode);
            ipn.setSignature(signature);
            ipn.setAmount(amount);
            ipn.setTransId(transId);
            ipn.setOrderInfo(orderInfo);
            ipn.setOrderType(orderType);
            ipn.setPayType(payType);
            ipn.setResponseTime(responseTime);
            ipn.setRequestId(requestId);
            ipn.setMessage(message);
            ipn.setExtraData(extraData);  // ← chứa residentId
            ipn.setPartnerCode("MOMO");
            momoService.handleIpn(ipn);
        }

        return ResponseEntity.status(302)
                .header("Location", "http://localhost:8080/resident/invoices"
                        + "?momoResult=" + resultCode)
                .build();
    }

    /**
     * GET /api/momo/status/{invoiceId}
     * Frontend polling để kiểm tra kết quả thanh toán.
     * (Annotation @GetMapping bị thiếu trong file cũ — đã thêm lại.)
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