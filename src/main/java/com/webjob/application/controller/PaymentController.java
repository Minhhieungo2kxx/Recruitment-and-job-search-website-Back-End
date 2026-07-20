package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Payments.MomoPaymentCallback;
import com.webjob.application.enums.PaymentStatus;
import com.webjob.application.dto.Request.Payments.PaymentCreateRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.PaymentResponse;
import com.webjob.application.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentCreateRequest request, HttpServletRequest httpRequest) {
        ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Tạo payment thành công",
                paymentService.createPaymentGateway(request, httpRequest)
        );

        return ResponseEntity.ok(apiResponse);

    }


    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleVNPayReturn(HttpServletRequest request) {
        PaymentResponse paymentResponse = paymentService.handleVNPayReturn(request);
        ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                PaymentStatus.SUCCESS.name()
                        .equalsIgnoreCase(paymentResponse.getStatus())
                        ? "Thanh toán thành công"
                        : "Thanh toán thất bại",
                paymentResponse
        );
        return ResponseEntity.ok(apiResponse);
    }


    @GetMapping("/momo-return")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleMomoReturn(MomoPaymentCallback callback) {
        PaymentResponse response = paymentService.handleMomoReturn(callback);
        ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                PaymentStatus.SUCCESS.name()
                        .equalsIgnoreCase(response.getStatus())
                        ? "Thanh toán thành công"
                        : "Thanh toán thất bại",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory() {
        ApiResponse<List<PaymentResponse>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy lịch sử thanh toán thành công",
                paymentService.getPaymentHistory()
        );

        return ResponseEntity.ok(apiResponse);


    }

}

