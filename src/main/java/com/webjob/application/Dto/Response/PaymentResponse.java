package com.webjob.application.Dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long userId;
    private Long jobId;
    private String jobName;
    private Long amount;
    private String status;
    private String transactionId;
    private String paymentUrl; // URL để redirect đến cổng thanh toán
    private String gateway;    // VNPay, MoMo, ZaloPay
    private String bankCode;   // Mã ngân hàng
    private String ResponseCode;
    private String OrderInfo;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant payDate;   // Thời gian thanh toán thành công
    @Size(max = 255)
    private String paymentGatewayResponse; // Raw response / info từ gateway
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant createdAt;
}
