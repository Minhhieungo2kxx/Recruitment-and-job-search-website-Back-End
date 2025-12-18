package com.webjob.application.Dto.Request.Payments;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoPaymentCallback {
    private String partnerCode;
    private String orderId;
    private String requestId;
    private String amount;
    private String orderInfo;
    private String transId;
    private String message;
    private String resultCode;
    private String payUrl;
    private String signature;
    private String responseTime;
    private String payType;
    private String orderType;
}
