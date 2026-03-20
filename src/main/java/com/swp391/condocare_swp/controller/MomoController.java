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
     * GET /api/momo/return?orderId=...&resultCode=0&...
     *
     * MoMo redirect user về URL này sau khi thanh toán xong.
     * Dùng khi KHÔNG có ngrok (localhost dev) — thay thế cho IPN.
     *
     * Security: dùng permitAll vì MoMo redirect trực tiếp,
     * nhưng ta verify signature trước khi update DB.
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
            // Tạo MomoIpnRequest từ query params để tái sử dụng handleIpn
            com.swp391.condocare_swp.dto.MomoIpnRequest ipn =
                    new com.swp391.condocare_swp.dto.MomoIpnRequest();
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
            ipn.setExtraData(extraData);
            ipn.setPartnerCode("MOMO");

            momoService.handleIpn(ipn); // tái sử dụng logic verify + update DB
        }

        // Redirect về trang hóa đơn (dù thành công hay thất bại)
        return ResponseEntity.status(302)
                .header("Location", "http://localhost:8080/resident/invoices"
                        + "?momoResult=" + resultCode)
                .build();
    }
    public ResponseEntity<?> checkStatus(@PathVariable String invoiceId) {
        try {
            return ResponseEntity.ok(momoService.checkPaymentStatus(invoiceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}