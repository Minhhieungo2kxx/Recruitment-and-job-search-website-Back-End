package com.webjob.application.Controller;

import com.webjob.application.Models.Entity.User;
import com.webjob.application.Models.Enums.PaymentStatus;
import com.webjob.application.Models.Request.PaymentCallbackRequest;
import com.webjob.application.Models.Request.PaymentCreateRequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.PaymentResponse;
import com.webjob.application.Services.PaymentService;
import com.webjob.application.Services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

@PostMapping("/create")
public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
        @Valid @RequestBody PaymentCreateRequest request,
        HttpServletRequest httpRequest) {

    try {
        // Lấy thông tin người dùng hiện tại từ context
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getbyEmail(userEmail);

        // Gọi service để tạo payment
        PaymentResponse paymentResponse = paymentService.createPaymentForJobView(
                currentUser.getId(), request, httpRequest
        );

        // Tạo phản hồi thành công
        ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Tạo payment thành công",
                paymentResponse
        );

        return ResponseEntity.ok(apiResponse);

    } catch (Exception ex) {
        log.error("Error creating payment: {}", ex.getMessage(), ex);

        // Tạo phản hồi lỗi
        ApiResponse<PaymentResponse> errorResponse = new ApiResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                "Lỗi tạo payment",
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}


    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleVNPayReturn(HttpServletRequest request) {
        try {
            // Parse VNPay callback parameters
            PaymentCallbackRequest callbackRequest =paymentService.extractVNPayCallbackParams(request);

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

        } catch (Exception ex) {
            log.error("Lỗi xử lý VNPay callback: {}", ex.getMessage(), ex);

            ApiResponse<PaymentResponse> errorResponse = new ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    ex.getMessage(),
                    "Lỗi xử lý callback",
                    null
            );

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory() {

        try {
            // Lấy user ID từ authentication (giả sử có getUserId method)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User userHR = userService.getbyEmail(email);
            Long userId = userHR.getId();
            List<PaymentResponse> history = paymentService.getUserPaymentHistory(userId);
            ApiResponse<?> apiResponse=new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    "Lấy lịch sử thanh toán thành công",
                    history
            );

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error getting payment history: {}", e.getMessage());
            ApiResponse<?> apiResponse=new ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    "Lỗi lấy lịch sử thanh toán: ",
                    null
            );
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }


}

