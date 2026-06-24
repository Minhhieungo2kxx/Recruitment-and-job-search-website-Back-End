package com.webjob.application.service;

import com.webjob.application.dto.Request.Payments.MomoPaymentCallback;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.record.PaymentSuccessEvent;
import com.webjob.application.exception.Customs.BusinessException;
import com.webjob.application.exception.Customs.PaymentExpiredException;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.Payment;
import com.webjob.application.models.Entity.User;
import com.webjob.application.enums.CompetitionLevel;
import com.webjob.application.enums.PaymentStatus;
import com.webjob.application.dto.Request.Payments.PaymentCallbackRequest;
import com.webjob.application.dto.Request.Payments.PaymentCreateRequest;
import com.webjob.application.dto.Response.JobApplicantInfoResponse;
import com.webjob.application.dto.Response.PaymentResponse;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.PaymentRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.PaymentGateway.MomoService;
import com.webjob.application.service.Redis.RedisLockService;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.service.PaymentGateway.VNPayService;
import com.webjob.application.utils.UtilFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final VNPayService vnPayService;

    private final UserService userService;
    private final ApplicationEmailService applicationEmailService;

    private final RedisLockService redisLockService;
    private final MomoService momoService;

    private static final Long COMPETITION_VIEW_PRICE =30000L;
    private final ApplicationEventPublisher eventPublisher;



    public PaymentResponse createPaymentVnpay(Long userId, PaymentCreateRequest request, HttpServletRequest httpRequest) {

        String lockKey = "payment:create:" + userId + ":" + request.getJobId();
        String lockValue = UtilFormat.generate8CharToken();

        boolean locked = redisLockService.tryLock(lockKey, lockValue,60, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("Bạn đang có giao dịch đang xử lý. Vui lòng chờ.");
        }

        try {
            // Lấy job từ DB, nếu không tồn tại thì báo lỗi
            Job job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy công việc"));

            // Lấy user từ DB
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Kiểm tra mức độ cạnh tranh của công việc
            if (job.getCompetitionLevel() != CompetitionLevel.HIGH) {
                throw new  BusinessException("Công việc này không yêu cầu thanh toán để xem thông tin ứng viên");
            }
            boolean hasPending = paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    userId, job.getId(), PaymentStatus.PENDING.name(),LocalDateTime.now()
            );
            if (hasPending) {
                throw new  BusinessException("Bạn đang có giao dịch đang xử lý cho công việc này. Vui lòng chờ.");
            }
            // Kiểm tra xem user đã thanh toán thành công cho job này chưa
            boolean hasPaid = paymentRepository.existsByUserIdAndJobIdAndStatus(userId, job.getId(), PaymentStatus.SUCCESS.name());
            if (hasPaid) {
                throw new BusinessException("Bạn đã thanh toán để xem thông tin ứng viên của công việc này");
            }
            // Tạo link thanh toán VNPay
            String orderInfo = String.format("Thanh toán xem thông tin ứng viên - Công việc: %s", job.getName());
            // Tạo bản ghi thanh toán
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setJob(job);
            payment.setAmount(COMPETITION_VIEW_PRICE);
            payment.setStatus(PaymentStatus.PENDING.name());
            payment.setExpiredAt(LocalDateTime.now().plusMinutes(15));
            // Tạo txnRef duy nhất dựa trên paymentId
            String txnRef = vnPayService.generateTxnRef(user.getId());
            payment.setOrderCode(txnRef);
            payment.setProvider(request.getGateway());
            payment.setOrderInfo(orderInfo);
            payment = paymentRepository.save(payment);


            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("amount", COMPETITION_VIEW_PRICE.intValue());
            dataMap.put("orderInfo", orderInfo);
            dataMap.put("userId", userId);
            dataMap.put("txnRef", txnRef);
            String paymentUrl = vnPayService.createOrder(httpRequest,dataMap);

            // Tạo response
            PaymentResponse response = new PaymentResponse();
            response.setId(payment.getId());
            response.setUserId(userId);
            response.setJobId(job.getId());
            response.setJobName(job.getName());
            response.setAmount(COMPETITION_VIEW_PRICE);
            response.setStatus(payment.getStatus());
            response.setPaymentUrl(paymentUrl);
            response.setCreatedAt(payment.getCreatedAt());

            log.info("Payment created | userId: {}, jobId: {}, paymentId: {}", userId, job.getId(), payment.getId());

            return response;

        }finally {
            // Luôn unlock dù success hay exception
            redisLockService.unlock(lockKey, lockValue); // unlock an toàn theo value

        }

    }



    public PaymentResponse handlePaymentCallbackVnPay(PaymentCallbackRequest callbackRequest, HttpServletRequest request) {
        // 1. Validate chữ ký từ VNPay
        int verifyResult = vnPayService.orderReturn(request);
        if (verifyResult != 1) {
            throw new RuntimeException("Chữ ký không hợp lệ từ VNPay");
        }

        // 3. Tìm payment record liên quan
        Payment payment = paymentRepository.findByOrderCode(callbackRequest.getVnp_TxnRef())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment cho Ordercode: " + callbackRequest.getVnp_TxnRef()));
        // 3. Nếu đã success → KHÔNG xử lý lại
        if (payment.getStatus().equals(PaymentStatus.SUCCESS.name())) {
            log.info("Callback duplicated - ignoring");
            return buildPaymentResponse(payment);
        }
        // 4. Kiểm tra số tiền
        long paidAmount = Long.parseLong(callbackRequest.getVnp_Amount()) / 100;
        if (!payment.getAmount().equals(paidAmount)) {
            throw new RuntimeException("Amount mismatch");
        }
        if (payment.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new PaymentExpiredException("Payment expired");
        }


        // 4. Cập nhật trạng thái thanh toán
        String status = (verifyResult == 1)
                ? PaymentStatus.SUCCESS.name()
                : PaymentStatus.FAILED.name();

        payment.setStatus(status);
        payment.setTransactionId(callbackRequest.getVnp_TransactionNo()); // Mã giao dịch thật từ VNPay
        payment.setBankCode(callbackRequest.getVnp_BankCode());
        payment.setSecureHash(callbackRequest.getVnp_SecureHash());
        String payDateStr = callbackRequest.getVnp_PayDate(); // "20251203143025"
        Instant payDate = UtilFormat.parseToInstant(payDateStr);
        payment.setPayDate(payDate);
        payment.setPaymentGatewayResponse("ResponseCode: " + callbackRequest.getVnp_ResponseCode() +
                ", TransactionStatus: " + callbackRequest.getVnp_TransactionStatus());
        payment.setResponseCode(callbackRequest.getVnp_ResponseCode());
        payment.setPayType("napas");
        payment.setOrderType("napas");

        payment = paymentRepository.save(payment);

        log.info("Payment {} for transactionRef: {}", status.toLowerCase(),callbackRequest.getVnp_TransactionNo());
        if(payment.getStatus().equals(PaymentStatus.SUCCESS.name())){
            applicationEmailService.sendPaymentEmail(payment);
        }
        // 5. Tạo và trả về response
        return buildPaymentResponse(payment);
    }




    public JobApplicantInfoResponse getJobApplicantInfo(Long userId, Long jobId) {
        // Kiểm tra job tồn tại
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        JobApplicantInfoResponse response = new JobApplicantInfoResponse();
        response.setJobId(job.getId());
        response.setJobName(job.getName());
        response.setCompetitionLevel(job.getCompetitionLevel());

        // Kiểm tra mức độ cạnh tranh
        if (job.getCompetitionLevel() == CompetitionLevel.HIGH) {
            // Cần thanh toán để xem
            boolean hasPaid = paymentRepository.existsByUserIdAndJobIdAndStatus(userId, jobId, "SUCCESS");
            response.setHasPaidAccess(hasPaid);

            if (hasPaid) {
                response.setAppliedCount(job.getAppliedCount());
                response.setMessage("Bạn có thể xem thông tin ứng viên");
            } else {
                response.setAppliedCount(-1); // Không hiển thị số lượng
                response.setMessage("Cần thanh toán " + COMPETITION_VIEW_PRICE + " VND để xem thông tin ứng viên");
            }
        } else {
            // Miễn phí xem
            response.setAppliedCount(job.getAppliedCount());
            response.setHasPaidAccess(true);
            response.setMessage("Thông tin ứng viên miễn phí");
        }

        return response;
    }

    public List<PaymentResponse> getUserPaymentHistory(Long userId) {
        List<Payment> payments = paymentRepository.findByUserIdAndStatus(userId, "SUCCESS");

        return payments.stream()
                .map(payment -> {
                    PaymentResponse response = new PaymentResponse();
                    response.setId(payment.getId());
                    response.setUserId(payment.getUser().getId());
                    response.setJobId(payment.getJob().getId());
                    response.setJobName(payment.getJob().getName());
                    response.setAmount(payment.getAmount());
                    response.setStatus(payment.getStatus());
                    response.setTransactionId(payment.getTransactionId());
                    response.setCreatedAt(payment.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }
    public PaymentCallbackRequest extractVNPayCallbackParams(HttpServletRequest request) {
        PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
        callbackRequest.setVnp_Amount(request.getParameter("vnp_Amount"));
        callbackRequest.setVnp_BankCode(request.getParameter("vnp_BankCode"));
        callbackRequest.setVnp_BankTranNo(request.getParameter("vnp_BankTranNo"));
        callbackRequest.setVnp_CardType(request.getParameter("vnp_CardType"));
        callbackRequest.setVnp_OrderInfo(request.getParameter("vnp_OrderInfo"));
        callbackRequest.setVnp_PayDate(request.getParameter("vnp_PayDate"));
        callbackRequest.setVnp_ResponseCode(request.getParameter("vnp_ResponseCode"));
        callbackRequest.setVnp_TmnCode(request.getParameter("vnp_TmnCode"));
        callbackRequest.setVnp_TransactionNo(request.getParameter("vnp_TransactionNo"));
        callbackRequest.setVnp_TransactionStatus(request.getParameter("vnp_TransactionStatus"));
        callbackRequest.setVnp_TxnRef(request.getParameter("vnp_TxnRef"));
        callbackRequest.setVnp_SecureHash(request.getParameter("vnp_SecureHash"));
        return callbackRequest;
    }

    private PaymentResponse buildPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setUserId(payment.getUser().getId());
        response.setJobId(payment.getJob().getId());
        response.setJobName(payment.getJob().getName());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setPayDate(payment.getPayDate());
        response.setGateway(payment.getProvider());
        response.setBankCode(payment.getBankCode());
        response.setPaymentGatewayResponse(payment.getPaymentGatewayResponse());
        response.setResponseCode(payment.getResponseCode());
        response.setOrderInfo(payment.getOrderInfo());
        response.setPayType(payment.getPayType());
        response.setOrderType(payment.getOrderType());

        return response;
    }


    public PaymentResponse createPaymentMomo(Long userId, PaymentCreateRequest request, HttpServletRequest httpRequest) {

        String lockKey = "payment:create:" + userId + ":" + request.getJobId();
        String lockValue = UtilFormat.generate8CharToken();

        boolean locked = redisLockService.tryLock(lockKey, lockValue,60, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("Bạn đang có giao dịch đang xử lý. Vui lòng chờ.");
        }
        try {
            // Lấy job từ DB, nếu không tồn tại thì báo lỗi
            Job job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

            // Lấy user từ DB
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Kiểm tra mức độ cạnh tranh của công việc
            if (job.getCompetitionLevel() != CompetitionLevel.HIGH) {
                throw new  BusinessException("Công việc này không yêu cầu thanh toán để xem thông tin ứng viên");
            }
            boolean hasPending = paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    userId, job.getId(), PaymentStatus.PENDING.name(),LocalDateTime.now()
            );
            if (hasPending) {
                throw new  BusinessException("Bạn đang có giao dịch đang xử lý cho công việc này. Vui lòng chờ.");
            }
            // Kiểm tra xem user đã thanh toán thành công cho job này chưa
            boolean hasPaid = paymentRepository.existsByUserIdAndJobIdAndStatus(userId, job.getId(), PaymentStatus.SUCCESS.name());
            if (hasPaid) {
                throw new BusinessException("Bạn đã thanh toán để xem thông tin ứng viên của công việc này");
            }
            // Tạo link thanh toán VNPay
            String orderInfo = String.format("Thanh toán xem thông tin ứng viên - Công việc: %s", job.getName());
            String requestId = "MOMO" + new Date().getTime();
            String paymentUrl = momoService.createPaymentRequest(COMPETITION_VIEW_PRICE.toString(),requestId);
            // Tạo bản ghi thanh toán
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setJob(job);
            payment.setAmount(COMPETITION_VIEW_PRICE);
            payment.setStatus(PaymentStatus.PENDING.name());
            payment.setExpiredAt(LocalDateTime.now().plusMinutes(15));

            payment.setOrderCode(requestId);
            payment.setProvider(request.getGateway());
            payment.setOrderInfo(orderInfo);
            payment = paymentRepository.save(payment);
            // Tạo response
            PaymentResponse response = new PaymentResponse();
            response.setId(payment.getId());
            response.setUserId(userId);
            response.setJobId(job.getId());
            response.setJobName(job.getName());
            response.setAmount(COMPETITION_VIEW_PRICE);
            response.setStatus(payment.getStatus());
            response.setPaymentUrl(paymentUrl);
            response.setCreatedAt(payment.getCreatedAt());
            log.info("Payment created | userId: {}, jobId: {}, paymentId: {}", userId, job.getId(), payment.getId());
            return response;

        }finally {
            // Luôn unlock dù success hay exception
            redisLockService.unlock(lockKey,lockValue);

        }

    }

    public PaymentResponse handlePaymentMomo(MomoPaymentCallback callbackRequest) {
        // 1. Validate chữ ký
        boolean validationResult=momoService.verifyMomoCallbackSignature(callbackRequest);
        if (!validationResult) {
            throw new RuntimeException("Chữ ký không hợp lệ");
        }
        // 3. Tìm payment record liên quan
        Payment payment = paymentRepository.findByOrderCode(callbackRequest.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment cho Ordercode: " + callbackRequest.getOrderId()));
        // 3. Nếu đã success → KHÔNG xử lý lại
        if (payment.getStatus().equals(PaymentStatus.SUCCESS.name())) {
            log.info("Callback duplicated - ignoring");
            return buildPaymentResponse(payment);
        }
        // 4. Kiểm tra số tiền
        long paidAmount = Long.parseLong(callbackRequest.getAmount());
        if (!payment.getAmount().equals(paidAmount)) {
            throw new RuntimeException("Amount mismatch");
        }
        if (payment.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new PaymentExpiredException("Payment expired");
        }

        // 4. Cập nhật trạng thái thanh toán
        String status = (validationResult ==true)
                ? PaymentStatus.SUCCESS.name()
                : PaymentStatus.FAILED.name();

        payment.setStatus(status);
        payment.setTransactionId(callbackRequest.getTransId()); // Mã giao dịch thật từ
        payment.setSecureHash(callbackRequest.getSignature());

        String responseTimeStr = callbackRequest.getResponseTime();
        long responseTime = Long.parseLong(responseTimeStr);
        Instant payDate = Instant.ofEpochMilli(responseTime);
        payment.setPayDate(payDate);
        payment.setPaymentGatewayResponse("ResponseCode: " + callbackRequest.getResultCode() +
                ", TransactionStatus: " + callbackRequest.getResultCode());
        payment.setResponseCode(callbackRequest.getResultCode());
        payment.setOrderType(callbackRequest.getOrderType());
        payment.setPayType(callbackRequest.getPayType());
        payment = paymentRepository.save(payment);
        log.info("Payment {} for transactionRef: {}", status.toLowerCase(),callbackRequest.getResultCode());
        if(payment.getStatus().equals(PaymentStatus.SUCCESS.name())){
//            applicationEmailService.sendPaymentEmail(payment);
            eventPublisher.publishEvent(
                    new PaymentSuccessEvent(payment.getId())
            );
        }
        // 5. Tạo và trả về response
        return buildPaymentResponse(payment);
    }

    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void expirePendingPayments() {
        List<Payment> expiredPayments =
                paymentRepository.findByStatusAndExpiredAtBefore(
                        PaymentStatus.PENDING.name(), LocalDateTime.now()
                );

        expiredPayments.forEach(p -> {
            p.setStatus(PaymentStatus.SUCCESS.EXPIRED.name());
            paymentRepository.save(p);
        });
    }
    @Scheduled(cron = "0 0 3 * * ?") // 3h sáng
    @Transactional
    public void cleanupOldPayments() {
        int deleted = paymentRepository.deleteOldPayments();
        log.info("Deleted {} old payments", deleted);
    }
    @Transactional
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment_Gateway(PaymentCreateRequest request,
            HttpServletRequest httpRequest, Authentication authentication) {
        User currentUser = userService.getById(Long.valueOf(authentication.getName()));
        PaymentResponse paymentResponse;

        if ("VNPAY".equals(request.getGateway())) {
            paymentResponse = createPaymentVnpay(
                    currentUser.getId(), request, httpRequest);
        } else {
            paymentResponse = createPaymentMomo(
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

    @Transactional
    public ResponseEntity<ApiResponse<PaymentResponse>> handle_VNPayReturn(HttpServletRequest request) {
        // Parse VNPay callback parameters
        PaymentCallbackRequest callbackRequest = extractVNPayCallbackParams(request);
        // Xử lý callback
        PaymentResponse response = handlePaymentCallbackVnPay(callbackRequest, request);
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

    @Transactional
    public ResponseEntity<?> handle_MomoReturn(MomoPaymentCallback callback) {

        PaymentResponse response = handlePaymentMomo(callback);
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
    public ResponseEntity<?> getPayment_History(Authentication authentication) {
        // Lấy user ID từ authentication (giả sử có getUserId method)
        User user = userService.getById(Long.valueOf(authentication.getName()));
        List<PaymentResponse> history = getUserPaymentHistory(user.getId());
        ApiResponse<?> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy lịch sử thanh toán thành công",
                history
        );

        return ResponseEntity.ok(apiResponse);
    }











}
