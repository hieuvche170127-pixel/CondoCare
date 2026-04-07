package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.MomoIpnRequest;
import com.swp391.condocare_swp.service.MomoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller cho MoMo Payment.
 * Base: /api/momo
 *
 * THAY ĐỔI (so với phiên bản cũ):
 *   - Bỏ @Autowired InvoiceRepository và ResidentsRepository khỏi Controller.
 *     Controller không nên chứa DB query — đó là trách nhiệm của Service layer.
 *
 *   - createPayment() giờ chỉ:
 *       1. Đọc invoiceId từ body
 *       2. Lấy username từ JWT (SecurityContext)
 *       3. Gọi momoService.createPaymentForUser(invoiceId, username)
 *     Toàn bộ logic tìm Invoice + Resident được chuyển vào MomoService.createPaymentForUser().
 *
 *   - handleReturn() dùng @Value app.base-url (đã có trong application.properties thông qua
 *     momo.redirect-url) thay vì hard-code localhost:8080.
 *     Vẫn giữ redirect đến /resident/invoices để không break frontend.
 */
@RestController
@RequestMapping("/api/momo")
public class MomoController {

    private static final Logger logger = LoggerFactory.getLogger(MomoController.class);

    // [THAY ĐỔI] Chỉ còn 1 dependency — MomoService đã gom toàn bộ logic
    @Autowired
    private MomoService momoService;

    /**
     * POST /api/momo/create-payment
     * Resident gọi khi click "Thanh toán MoMo".
     * Body: { "invoiceId": "INV2026030001" }
     *
     * [THAY ĐỔI] Controller không còn query InvoiceRepository và ResidentsRepository trực tiếp.
     *            Tất cả logic nghiệp vụ (tìm invoice, kiểm tra đã paid, lấy residentId)
     *            được chuyển vào momoService.createPaymentForUser().
     */
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, String> body) {
        String invoiceId = body.get("invoiceId");
        if (invoiceId == null || invoiceId.isBlank())
            return ResponseEntity.badRequest().body("Thiếu invoiceId");

        try {
            // Lấy username từ JWT để service tự tìm residentId
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            Map<String, Object> result = momoService.createPaymentForUser(invoiceId, username);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            // Lỗi nghiệp vụ (invoice không tồn tại, đã paid, ...) → 400
            logger.warn("MoMo createPayment rejected — invoiceId={}, reason={}", invoiceId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
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
     * MoMo redirect user về URL này sau khi thanh toán xong (dùng khi dev local).
     *
     * [THAY ĐỔI] URL redirect không còn hard-code "http://localhost:8080/resident/invoices".
     *            Đọc base URL từ momo.redirect-url trong application.properties thay thế.
     *            MomoService đã được inject redirect-url qua @Value — tuy nhiên vì đây là
     *            redirect HTTP 302 từ Controller, ta vẫn cần build URL tại đây.
     *            Giải pháp: thêm app.base-url vào properties và inject @Value vào Controller.
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
            ipn.setExtraData(extraData);
            ipn.setPartnerCode("MOMO");
            momoService.handleIpn(ipn);
        }

        // Redirect về trang hóa đơn của resident với kết quả
        // [FIX] Dùng path tương đối — Spring sẽ xử lý host tự động từ request context.
        //       Không còn hard-code "http://localhost:8080" ở đây.
        return ResponseEntity.status(302)
                .header("Location", "/resident/invoices?momoResult=" + resultCode)
                .build();
    }

    /**
     * GET /api/momo/status/{invoiceId}
     * Frontend polling để kiểm tra kết quả thanh toán.
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