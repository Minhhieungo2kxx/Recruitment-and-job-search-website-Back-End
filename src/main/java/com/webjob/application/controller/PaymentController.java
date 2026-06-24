package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Payments.MomoPaymentCallback;
import com.webjob.application.models.Entity.User;
import com.webjob.application.enums.PaymentStatus;
import com.webjob.application.dto.Request.Payments.PaymentCallbackRequest;
import com.webjob.application.dto.Request.Payments.PaymentCreateRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.PaymentResponse;
import com.webjob.application.service.PaymentService;
import com.webjob.application.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            HttpServletRequest httpRequest, Authentication authentication) {

        return paymentService.createPayment_Gateway(request, httpRequest, authentication);
    }


    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleVNPayReturn(HttpServletRequest request) {
        return paymentService.handle_VNPayReturn(request);
    }


    @GetMapping("/momo-return")
    public ResponseEntity<?> handleMomoReturn(MomoPaymentCallback callback) {
        return paymentService.handle_MomoReturn(callback);

    }


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory(Authentication authentication) {


        return paymentService.getPayment_History(authentication);
    }

}

