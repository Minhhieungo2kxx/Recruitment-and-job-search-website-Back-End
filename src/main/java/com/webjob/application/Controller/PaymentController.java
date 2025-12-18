package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Dto.Request.Payments.MomoPaymentCallback;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Model.Enums.PaymentStatus;
import com.webjob.application.Dto.Request.Payments.PaymentCallbackRequest;
import com.webjob.application.Dto.Request.Payments.PaymentCreateRequest;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.PaymentResponse;
import com.webjob.application.Service.PaymentService;
import com.webjob.application.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User currentUser = userService.getbyEmail(userEmail);

        PaymentResponse paymentResponse;

        if ("VNPAY".equals(request.getGateway())) {
            paymentResponse = paymentService.createPaymentForJobView(
                    currentUser.getId(), request, httpRequest);
        } else {
            paymentResponse = paymentService.createPaymentMomo(
                    currentUser.getId(), request, httpRequest);
        }

        ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Tạo payment thành công",
                paymentResponse
        );

        return ResponseEntity.ok(apiResponse);
    }



    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleVNPayReturn(HttpServletRequest request) {
            // Parse VNPay callback parameters
            PaymentCallbackRequest callbackRequest = paymentService.extractVNPayCallbackParams(request);
            // Xử lý callback
            PaymentResponse response = paymentService.handlePaymentCallback(callbackRequest, request);
            // Xác định thông điệp phản hồi
            String message = PaymentStatus.SUCCESS.name().equalsIgnoreCase(response.getStatus())
                    ? "Thanh toán thành công"
                    : "Thanh toán thất bại";

            ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    message,
                    response
            );
            return ResponseEntity.ok(apiResponse);
    }


    @GetMapping("/momo-return")
    public ResponseEntity<?> handleMomoReturn(HttpServletRequest request) {
            MomoPaymentCallback callbackRequest = paymentService.extractMomoCallbackParams(request);
            PaymentResponse response = paymentService.handlePayment(callbackRequest,request);
            // Xác định thông điệp phản hồi
            String message = PaymentStatus.SUCCESS.name().equalsIgnoreCase(response.getStatus())
                    ? "Thanh toán thành công"
                    : "Thanh toán thất bại";

            ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    message,
                    response
            );
            return ResponseEntity.ok(apiResponse);

    }


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory() {
            // Lấy user ID từ authentication (giả sử có getUserId method)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.getbyEmail(email);
            List<PaymentResponse> history = paymentService.getUserPaymentHistory(user.getId());
            ApiResponse<?> apiResponse = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    "Lấy lịch sử thanh toán thành công",
                    history
            );

            return ResponseEntity.ok(apiResponse);
    }

}

