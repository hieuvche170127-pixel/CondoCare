package com.swp391.condocare_swp.dto;

import lombok.Data;

/**
 * Request gửi lên MoMo API để tạo payment
 * Docs: https://developers.momo.vn/v3/docs/payment/api/pay-gate/
 */
@Data
public class MomoCreatePaymentRequest {
    private String partnerCode;
    private String partnerName;
    private String storeId;
    private String requestId;
    private long   amount;
    private String orderId;
    private String orderInfo;
    private String redirectUrl;
    private String ipnUrl;
    private String lang;
    private String requestType;
    private String extraData;
    private String signature;
}