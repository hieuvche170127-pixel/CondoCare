package com.swp391.condocare_swp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.condocare_swp.dto.MomoCreatePaymentRequest;
import com.swp391.condocare_swp.dto.MomoCreatePaymentResponse;
import com.swp391.condocare_swp.dto.MomoIpnRequest;
import com.swp391.condocare_swp.entity.Invoice;
import com.swp391.condocare_swp.entity.Payments;
import com.swp391.condocare_swp.repository.PaymentsRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
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
 *
 * ── Thay đổi so với phiên bản cũ ────────────────────────────
 * - createPayment() nhận thêm residentId, lưu vào extraData của request MoMo.
 * - handleIpn() đọc extraData để lấy residentId → gán paid_by trực tiếp,
 *   KHÔNG còn tìm ngược findFirstByApartment_Id (method không tồn tại).
 * - ResidentsRepository KHÔNG cần thêm method mới.
 */
@Service
public class MomoService {

    private static final Logger logger = LoggerFactory.getLogger(MomoService.class);

    @Value("${momo.partner-code:MOMO}")
    private String partnerCode;

    @Value("${momo.access-key:F8BBA842ECF85}")
    private String accessKey;

    @Value("${momo.secret-key:K951B6PE1waDMi640xX08PD3vg6EkVlz}")
    private String secretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    @Value("${momo.redirect-url:http://localhost:8080/resident/invoices}")
    private String redirectUrl;

    @Value("${momo.ipn-url:http://localhost:8080/api/momo/ipn}")
    private String ipnUrl;

    @Autowired private InvoiceRepository   invoiceRepo;
    @Autowired private PaymentsRepository  paymentsRepo;
    @Autowired private ResidentsRepository residentsRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newHttpClient();

    /* ══════════════════════════════════════════════════════
       TẠO PAYMENT REQUEST
    ══════════════════════════════════════════════════════ */

    /**
     * Tạo MoMo payment link cho một Invoice.
     *
     * @param invoiceId  ID hóa đơn (dùng làm orderId gốc)
     * @param amount     Số tiền (VND)
     * @param residentId ID cư dân đang thanh toán — lưu vào extraData để IPN đọc lại
     */
    public Map<String, Object> createPayment(String invoiceId, BigDecimal amount, String residentId) {
        String requestId  = UUID.randomUUID().toString();
        long   amountLong = amount.longValue();
        String orderInfo  = "Thanh toan hoa don " + invoiceId;

        // Lưu residentId vào extraData để IPN callback đọc lại khi gán paid_by.
        // Không encode Base64 để tránh phức tạp — MoMo chấp nhận plaintext.
        String extraData = (residentId != null && !residentId.isBlank()) ? residentId : "";

        // Thêm timestamp suffix để tránh duplicate orderId khi user bấm nhiều lần
        String orderId = invoiceId + "_" + System.currentTimeMillis();

        String requestType = "payWithMethod";

        String rawSignature = buildRawSignature(
                accessKey, String.valueOf(amountLong), extraData,
                ipnUrl, orderId, orderInfo, partnerCode,
                redirectUrl, requestId, requestType
        );

        String signature = hmacSHA256(secretKey, rawSignature);

        MomoCreatePaymentRequest req = new MomoCreatePaymentRequest();
        req.setPartnerCode(partnerCode);
        req.setPartnerName("CondoCare Apartment");
        req.setStoreId("CondoCare_Main");
        req.setRequestId(requestId);
        req.setAmount(amountLong);
        req.setOrderId(orderId);
        req.setOrderInfo(orderInfo);
        req.setRedirectUrl(redirectUrl);
        req.setIpnUrl(ipnUrl);
        req.setLang("vi");
        req.setRequestType(requestType);
        req.setExtraData(extraData);   // ← residentId
        req.setSignature(signature);

        try {
            String body = objectMapper.writeValueAsString(req);
            logger.info("MoMo create payment — orderId={}, amount={}, residentId={}",
                    orderId, amountLong, residentId);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(momoEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> httpRes = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            MomoCreatePaymentResponse momoRes =
                    objectMapper.readValue(httpRes.body(), MomoCreatePaymentResponse.class);

            logger.info("MoMo response — resultCode={}, message={}",
                    momoRes.getResultCode(), momoRes.getMessage());

            if (momoRes.getResultCode() != 0) {
                throw new RuntimeException("MoMo từ chối thanh toán: " + momoRes.getMessage()
                        + " (code=" + momoRes.getResultCode() + ")");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("payUrl",    momoRes.getPayUrl());
            result.put("qrCodeUrl", momoRes.getQrCodeUrl());
            result.put("deeplink",  momoRes.getDeeplink());
            result.put("shortLink", momoRes.getShortLink());
            result.put("orderId",   invoiceId);
            result.put("requestId", requestId);
            result.put("amount",    amountLong);
            return result;

        } catch (IOException | InterruptedException e) {
            logger.error("Error calling MoMo API", e);
            throw new RuntimeException("Không thể kết nối MoMo: " + e.getMessage());
        }
    }

    /* ══════════════════════════════════════════════════════
       XỬ LÝ IPN CALLBACK
    ══════════════════════════════════════════════════════ */

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
            return true;
        }

        // 3. Tách invoiceId thực từ orderId (orderId = invoiceId + "_" + timestamp)
        String rawOrderId = ipn.getOrderId();
        String invoiceId  = rawOrderId.contains("_")
                ? rawOrderId.substring(0, rawOrderId.lastIndexOf('_'))
                : rawOrderId;

        // 4. Đọc residentId từ extraData (được lưu lúc tạo payment)
        String residentId = (ipn.getExtraData() != null && !ipn.getExtraData().isBlank())
                ? ipn.getExtraData().trim()
                : null;

        invoiceRepo.findById(invoiceId).ifPresentOrElse(invoice -> {
            if (invoice.getStatus() != Invoice.InvoiceStatus.PAID) {
                invoice.setStatus(Invoice.InvoiceStatus.PAID);
                invoice.setPaidAt(LocalDateTime.now());
                invoiceRepo.save(invoice);
                logger.info("Invoice {} marked PAID via MoMo IPN — transId={}, payType={}",
                        invoiceId, ipn.getTransId(), ipn.getPayType());

                savePaymentRecord(invoice, ipn, residentId);
            } else {
                logger.info("Invoice {} already PAID, skipping IPN update", invoiceId);
            }
        }, () -> logger.warn("MoMo IPN — Invoice not found: {}", invoiceId));

        return true;
    }

    /**
     * Lưu bản ghi Payments sau khi IPN xác nhận thành công.
     * residentId lấy từ extraData — không cần query thêm DB theo apartment.
     */
    private void savePaymentRecord(Invoice invoice, MomoIpnRequest ipn, String residentId) {
        String invoiceId = invoice.getId();
        try {
            if (paymentsRepo.existsByInvoiceId(invoiceId)) {
                logger.info("Payment record already exists for invoice {}, skipping", invoiceId);
                return;
            }

            // Validate residentId tồn tại trong DB trước khi gán
            if (residentId == null || !residentsRepo.existsById(residentId)) {
                logger.warn("Skipped Payments save — residentId '{}' từ extraData không hợp lệ " +
                        "hoặc không tồn tại (invoiceId={})", residentId, invoiceId);
                return;
            }

            Payments payment = new Payments();
            payment.setId("PMT_" + System.currentTimeMillis());
            payment.setInvoiceId(invoiceId);
            payment.setAmount(invoice.getTotalAmount());
            payment.setPaidAt(LocalDateTime.now());
            payment.setMethod(Payments.PaymentMethod.MOMO);
            payment.setMomoTransId(String.valueOf(ipn.getTransId()));
            payment.setMomoOrderId(ipn.getOrderId());
            payment.setNote("MoMo - " + (ipn.getPayType() != null ? ipn.getPayType() : "online"));
            payment.setPaidBy(residentId);  // ← lấy từ extraData, không query thêm

            paymentsRepo.save(payment);
            logger.info("Payment record saved — invoiceId={}, transId={}, paidBy={}",
                    invoiceId, ipn.getTransId(), residentId);

        } catch (Exception ex) {
            // Không để lỗi Payments rollback Invoice đã PAID
            logger.warn("Could not save Payments record for invoice {}: {}", invoiceId, ex.getMessage());
        }
    }

    /* ══════════════════════════════════════════════════════
       QUERY PAYMENT STATUS
    ══════════════════════════════════════════════════════ */

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

    private String buildRawSignature(
            String accessKey, String amount, String extraData,
            String ipnUrl, String orderId, String orderInfo,
            String partnerCode, String redirectUrl, String requestId, String requestType) {
        return "accessKey="   + accessKey
                + "&amount="     + amount
                + "&extraData="  + extraData
                + "&ipnUrl="     + ipnUrl
                + "&orderId="    + orderId
                + "&orderInfo="  + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId="  + requestId
                + "&requestType=" + requestType;
    }

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

    private String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 error: " + e.getMessage(), e);
        }
    }
}