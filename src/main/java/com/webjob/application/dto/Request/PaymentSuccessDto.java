package com.webjob.application.dto.Request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentSuccessDto {
    private String email;
    private String userName;
    private String jobName;
    private String amount;
    private String status;
    private String transactionId;
    private String payDate;
    private String gateway;
    private String bankCode;
    private String responseCode;
    private String orderInfo;
    private String payType;
    private String orderType;
}
