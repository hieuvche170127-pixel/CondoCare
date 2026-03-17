package com.swp391.condocare_swp.dto;

import lombok.Data;

/**
 * Response từ MoMo API sau khi tạo payment
 */
@Data
public class MomoCreatePaymentResponse {
    private String partnerCode;
    private String requestId;
    private String orderId;
    private long   amount;
    private long   responseTime;
    private String message;
    private int    resultCode;   // 0 = thành công
    private String payUrl;       // URL redirect để user thanh toán
    private String deeplink;     // deeplink cho mobile app
    private String qrCodeUrl;    // QR code image URL
    private String shortLink;    // link rút gọn
}