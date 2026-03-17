package com.swp391.condocare_swp.dto;

import lombok.Data;

/**
 * IPN (Instant Payment Notification) payload từ MoMo gọi về server
 * MoMo POST đến ipnUrl sau khi user thanh toán xong
 */
@Data
public class MomoIpnRequest {
    private String partnerCode;
    private String orderId;       // = invoiceId mà chúng ta đặt khi tạo payment
    private String requestId;
    private long   amount;
    private String orderInfo;
    private String orderType;
    private long   transId;       // MoMo transaction ID
    private int    resultCode;    // 0 = thanh toán thành công
    private String message;
    private String payType;       // qr / webApp / miniapp / napas ...
    private long   responseTime;
    private String extraData;
    private String signature;
}