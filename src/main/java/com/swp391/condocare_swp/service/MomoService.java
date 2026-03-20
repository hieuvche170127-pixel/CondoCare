package com.swp391.condocare_swp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.condocare_swp.dto.MomoCreatePaymentRequest;
import com.swp391.condocare_swp.dto.MomoCreatePaymentResponse;
import com.swp391.condocare_swp.dto.MomoIpnRequest;
import com.swp391.condocare_swp.entity.Invoice;
import com.swp391.condocare_swp.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service tích hợp MoMo Payment Gateway
 *
 * Docs: https://developers.momo.vn/v3/docs/payment/api/pay-gate/
 *
 * ── Sandbox credentials (thay bằng LIVE khi production) ──────
 * partnerCode : MOMO
 * accessKey   : F8BBA842ECF85
 * secretKey   : K951B6PE1waDMi640xX08PD3vg6EkVlz
 * endpoint    : https://test-payment.momo.vn/v2/gateway/api/create
 */
@Service
public class MomoService {

    private static final Logger logger = LoggerFactory.getLogger(MomoService.class);

    // ── Config từ application.properties ─────────────────────
    @Value("${momo.partner-code:MOMO}")
    private String partnerCode;

    @Value("${momo.access-key:F8BBA842ECF85}")
    private String accessKey;

    @Value("${momo.secret-key:K951B6PE1waDMi640xX08PD3vg6EkVlz}")
    private String secretKey;

    /**
     * Endpoint sandbox: https://test-payment.momo.vn/v2/gateway/api/create
     * Endpoint production: https://payment.momo.vn/v2/gateway/api/create
     */
    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    /**
     * URL MoMo redirect sau khi thanh toán xong (trang kết quả)
     * Ví dụ: https://yourdomain.com/resident/invoices
     */
    @Value("${momo.redirect-url:http://localhost:8080/resident/invoices}")
    private String redirectUrl;

    /**
     * URL MoMo gọi về để thông báo kết quả (IPN)
     * Phải là URL public — dùng ngrok khi dev local
     * Ví dụ: https://abc.ngrok.io/api/momo/ipn
     */
    @Value("${momo.ipn-url:http://localhost:8080/api/momo/ipn}")
    private String ipnUrl;

    @Autowired private InvoiceRepository invoiceRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newHttpClient();

    /* ══════════════════════════════════════════════════════
       TẠO PAYMENT REQUEST
    ══════════════════════════════════════════════════════ */

    /**
     * Tạo MoMo payment link cho một Invoice.
     * Trả về Map chứa payUrl, qrCodeUrl, deeplink.
     *
     * @param invoiceId ID hóa đơn (dùng làm orderId)
     * @param amount    Số tiền (VND)
     */
    public Map<String, Object> createPayment(String invoiceId, BigDecimal amount) {
        String requestId = UUID.randomUUID().toString();
        long   amountLong = amount.longValue();
        String orderInfo  = "Thanh toan hoa don " + invoiceId;
        String extraData  = "";
        String requestType = "payWithMethod";

        // FIX: Thêm timestamp suffix để tránh duplicate orderId
        // khi user bấm thanh toán nhiều lần cho cùng 1 invoice
        String orderId = invoiceId + "_" + System.currentTimeMillis();

        // 1. Build raw signature string (key=value theo thứ tự alphabet)
        String rawSignature = buildRawSignature(
                accessKey, String.valueOf(amountLong), extraData,
                ipnUrl, orderId, orderInfo, partnerCode,
                redirectUrl, requestId, requestType
        );

        // 2. HMAC-SHA256
        String signature = hmacSHA256(secretKey, rawSignature);

        // 3. Build request body
        MomoCreatePaymentRequest req = new MomoCreatePaymentRequest();
        req.setPartnerCode(partnerCode);
        req.setPartnerName("CondoCare Apartment");
        req.setStoreId("CondoCare_Main");
        req.setRequestId(requestId);
        req.setAmount(amountLong);
        req.setOrderId(orderId);           // dùng orderId có suffix timestamp
        req.setOrderInfo(orderInfo);
        req.setRedirectUrl(redirectUrl);
        req.setIpnUrl(ipnUrl);
        req.setLang("vi");
        req.setRequestType(requestType);
        req.setExtraData(extraData);
        req.setSignature(signature);

        // 4. Gọi MoMo API
        try {
            String body = objectMapper.writeValueAsString(req);
            logger.info("MoMo create payment request — orderId={}, amount={}", invoiceId, amountLong);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(momoEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> httpRes = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            MomoCreatePaymentResponse momoRes = objectMapper.readValue(httpRes.body(), MomoCreatePaymentResponse.class);

            logger.info("MoMo response — resultCode={}, message={}", momoRes.getResultCode(), momoRes.getMessage());

            if (momoRes.getResultCode() != 0) {
                throw new RuntimeException("MoMo từ chối thanh toán: " + momoRes.getMessage()
                        + " (code=" + momoRes.getResultCode() + ")");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("payUrl",     momoRes.getPayUrl());
            result.put("qrCodeUrl",  momoRes.getQrCodeUrl());
            result.put("deeplink",   momoRes.getDeeplink());
            result.put("shortLink",  momoRes.getShortLink());
            result.put("orderId",    invoiceId);
            result.put("requestId",  requestId);
            result.put("amount",     amountLong);
            return result;

        } catch (IOException | InterruptedException e) {
            logger.error("Error calling MoMo API", e);
            throw new RuntimeException("Không thể kết nối MoMo: " + e.getMessage());
        }
    }

    /* ══════════════════════════════════════════════════════
       XỬ LÝ IPN CALLBACK (MoMo gọi về sau khi user TT)
    ══════════════════════════════════════════════════════ */

    /**
     * Xác minh IPN từ MoMo và cập nhật trạng thái Invoice.
     * Return true nếu hợp lệ và đã xử lý, false nếu signature sai.
     */
    @Transactional
    public boolean handleIpn(MomoIpnRequest ipn) {
        logger.info("MoMo IPN received — orderId={}, resultCode={}, transId={}",
                ipn.getOrderId(), ipn.getResultCode(), ipn.getTransId());

        // 1. Verify signature
        String rawSignature = buildIpnRawSignature(ipn);
        String expectedSig  = hmacSHA256(secretKey, rawSignature);

        if (!expectedSig.equals(ipn.getSignature())) {
            logger.warn("MoMo IPN signature INVALID — expected={}, got={}",
                    expectedSig, ipn.getSignature());
            return false;
        }

        // 2. Chỉ xử lý khi resultCode = 0 (thanh toán thành công)
        if (ipn.getResultCode() != 0) {
            logger.info("MoMo IPN — payment NOT successful, resultCode={}", ipn.getResultCode());
            return true; // Signature hợp lệ nhưng không cần update
        }

        // 3. Cập nhật Invoice → PAID
        // orderId có dạng "INV202601A102_1234567890" → tách lấy invoiceId thực
        String rawOrderId = ipn.getOrderId();
        String invoiceId  = rawOrderId.contains("_")
                ? rawOrderId.substring(0, rawOrderId.lastIndexOf('_'))
                : rawOrderId;

        invoiceRepo.findById(invoiceId).ifPresentOrElse(invoice -> {
            if (invoice.getStatus() != Invoice.InvoiceStatus.PAID) {
                invoice.setStatus(Invoice.InvoiceStatus.PAID);
                invoice.setPaidAt(LocalDateTime.now());
                invoiceRepo.save(invoice);
                logger.info("Invoice {} marked PAID via MoMo IPN — transId={}, payType={}",
                        invoiceId, ipn.getTransId(), ipn.getPayType());
            } else {
                logger.info("Invoice {} already PAID, skipping IPN update", invoiceId);
            }
        }, () -> logger.warn("MoMo IPN — Invoice not found: {}", invoiceId));

        return true;
    }

    /* ══════════════════════════════════════════════════════
       QUERY PAYMENT STATUS (polling từ frontend)
    ══════════════════════════════════════════════════════ */

    /**
     * Kiểm tra trạng thái thanh toán của một invoice.
     * Frontend polling mỗi 3 giây sau khi redirect về.
     */
    public Map<String, Object> checkPaymentStatus(String invoiceId) {
        Invoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + invoiceId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invoiceId", invoiceId);
        result.put("status",    invoice.getStatus().name());
        result.put("paid",      invoice.getStatus() == Invoice.InvoiceStatus.PAID);
        result.put("paidAt",    invoice.getPaidAt());
        return result;
    }

    /* ══════════════════════════════════════════════════════
       PRIVATE HELPERS
    ══════════════════════════════════════════════════════ */

    /**
     * Raw signature cho create payment — theo thứ tự alphabet của key
     * Tham khảo: https://developers.momo.vn/v3/docs/payment/api/pay-gate/#signature
     */
    private String buildRawSignature(
            String accessKey, String amount, String extraData,
            String ipnUrl, String orderId, String orderInfo,
            String partnerCode, String redirectUrl, String requestId, String requestType) {
        return "accessKey="  + accessKey
                + "&amount="    + amount
                + "&extraData=" + extraData
                + "&ipnUrl="    + ipnUrl
                + "&orderId="   + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }

    /**
     * Raw signature cho IPN verification
     */
    private String buildIpnRawSignature(MomoIpnRequest ipn) {
        return "accessKey="   + accessKey
                + "&amount="     + ipn.getAmount()
                + "&extraData="  + (ipn.getExtraData() != null ? ipn.getExtraData() : "")
                + "&message="    + ipn.getMessage()
                + "&orderId="    + ipn.getOrderId()
                + "&orderInfo="  + ipn.getOrderInfo()
                + "&orderType="  + (ipn.getOrderType() != null ? ipn.getOrderType() : "")
                + "&partnerCode=" + ipn.getPartnerCode()
                + "&payType="    + (ipn.getPayType() != null ? ipn.getPayType() : "")
                + "&requestId="  + ipn.getRequestId()
                + "&responseTime=" + ipn.getResponseTime()
                + "&resultCode=" + ipn.getResultCode()
                + "&transId="    + ipn.getTransId();
    }

    /**
     * HMAC-SHA256 signature
     */
    private String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // Convert bytes to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 error: " + e.getMessage(), e);
        }
    }
}