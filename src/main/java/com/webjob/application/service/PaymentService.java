package com.webjob.application.service;

import com.webjob.application.dto.Request.NotificationRequest;
import com.webjob.application.dto.Request.Payments.MomoPaymentCallback;
import com.webjob.application.dto.record.PaymentSuccessEvent;
import com.webjob.application.enums.NotificationType;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.BusinessException;
import com.webjob.application.exception.Customs.PaymentExpiredException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
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
import com.webjob.application.component.SecurityUtils;
import com.webjob.application.utils.common.UtilFormat;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
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

    private final SecurityUtils securityUtils;
    private static final Long COMPETITION_VIEW_PRICE = 30000L;
    private final ApplicationEventPublisher eventPublisher;

    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse createPaymentGateway(PaymentCreateRequest request, HttpServletRequest httpRequest) {
        User currentUser = securityUtils.getCurrentUser();

        if ("VNPAY".equals(request.getGateway())) {
            return createPaymentVnpay(
                    currentUser.getId(), request, httpRequest
            );
        }
        if ("MOMO".equals(request.getGateway())) {
            return createPaymentMomo(
                    currentUser.getId(), request, httpRequest
            );
        }
        throw new BusinessException("Cổng thanh toán không hợp lệ");
    }


    public PaymentResponse createPaymentVnpay(Long userId,PaymentCreateRequest request,
                                              HttpServletRequest httpRequest) {

        String lockKey = "payment:create:" + userId + ":" + request.getJobId();
        String lockValue = UtilFormat.generate8CharToken();

        boolean locked = redisLockService.tryLock(lockKey, lockValue, 2, TimeUnit.MINUTES);
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

            validatePaymentForJob(userId, job);

            Payment payment = createPayment(user, job, request.getGateway());

            Map<String, Object> dataMap = buildPaymentData(payment, userId);

            String paymentUrl = vnPayService.createOrder(httpRequest, dataMap);
            log.info(
                    "Payment created | userId: {}, jobId: {}, paymentId: {}",
                    userId, job.getId(), payment.getId()
            );
            return buildPaymentResponse(payment, userId, job, paymentUrl);

        } finally {
            // Luôn unlock dù success hay exception
            redisLockService.unlock(lockKey, lockValue); // unlock an toàn theo value

        }
    }

    private void validatePaymentForJob(Long userId, Job job) {
        if (job.getCompetitionLevel() != CompetitionLevel.HIGH) {
            throw new BusinessException("Công việc này không yêu cầu thanh toán để xem thông tin ứng viên");
        }
        boolean hasPending = paymentRepository
                .existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                        userId,
                        job.getId(),
                        PaymentStatus.PENDING.name(),
                        LocalDateTime.now()
                );

        if (hasPending) {
            throw new BusinessException("Bạn đang có giao dịch đang xử lý cho công việc này. Vui lòng chờ."
            );
        }
        boolean hasPaid = paymentRepository
                .existsByUserIdAndJobIdAndStatus(
                        userId,
                        job.getId(),
                        PaymentStatus.SUCCESS.name()
                );

        if (hasPaid) {
            throw new BusinessException(
                    "Bạn đã thanh toán để xem thông tin ứng viên của công việc này"
            );
        }
    }
    private Payment createPayment(User user, Job job, String gateway) {
        // Thông tin đơn hàng
        String orderInfo = String.format(
                "Thanh toán xem thông tin ứng viên - Công việc: %s",
                job.getName()
        );

        // Khởi tạo bản ghi thanh toán
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setJob(job);
        payment.setAmount(COMPETITION_VIEW_PRICE);
        payment.setStatus(PaymentStatus.PENDING.name());
        payment.setExpiredAt(LocalDateTime.now().plusMinutes(15));

        // Tạo mã giao dịch duy nhất
        String txnRef = vnPayService.generateTxnRef(user.getId());
        payment.setOrderCode(txnRef);

        payment.setProvider(gateway);
        payment.setOrderInfo(orderInfo);

        return paymentRepository.save(payment);
    }
    private Map<String, Object> buildPaymentData(Payment payment, Long userId) {
        Map<String, Object> dataMap = new HashMap<>();

        dataMap.put("amount", COMPETITION_VIEW_PRICE.intValue());
        dataMap.put("orderInfo", payment.getOrderInfo());
        dataMap.put("userId", userId);
        dataMap.put("txnRef", payment.getOrderCode());

        return dataMap;
    }
    private PaymentResponse buildPaymentResponse(
            Payment payment,
            Long userId,
            Job job,
            String paymentUrl
    ) {
        PaymentResponse response = new PaymentResponse();

        response.setId(payment.getId());
        response.setUserId(userId);
        response.setJobId(job.getId());
        response.setJobName(job.getName());
        response.setAmount(COMPETITION_VIEW_PRICE);
        response.setStatus(payment.getStatus());
        response.setPaymentUrl(paymentUrl);
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }

    @Transactional
    public PaymentResponse handleVNPayReturn(HttpServletRequest request) {
        // Parse VNPay callback parameters
        PaymentCallbackRequest callbackRequest = extractVNPayCallbackParams(request);
        // Xử lý callback
        PaymentResponse response = handlePaymentCallbackVnPay(callbackRequest, request);
        return response;
    }

    public PaymentResponse handlePaymentCallbackVnPay(PaymentCallbackRequest callbackRequest, HttpServletRequest request) {
        // 1. Validate chữ ký từ VNPay
        int verifyResult = vnPayService.orderReturn(request);
        if (verifyResult != 1) {
            throw new BadRequestException("Chữ ký không hợp lệ từ VNPay");
        }

        // 3. Tìm payment record liên quan
        Payment payment = paymentRepository.findByOrderCode(callbackRequest.getVnp_TxnRef())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment cho Ordercode: " + callbackRequest.getVnp_TxnRef()));
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
            payment.setStatus(PaymentStatus.EXPIRED.name());
            paymentRepository.save(payment);
            throw new PaymentExpiredException("Payment expired");
        }


        if ("00".equals(callbackRequest.getVnp_ResponseCode())) {
            payment.setStatus(PaymentStatus.SUCCESS.name());
        } else {
            payment.setStatus(PaymentStatus.FAILED.name());
        }

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

        log.info("Payment {} for transactionRef: {}", payment.getStatus(), callbackRequest.getVnp_TransactionNo());
        if (payment.getStatus().equals(PaymentStatus.SUCCESS.name())) {
            applicationEmailService.sendPaymentEmail(payment);
            sendPaymentNotification(payment);
        }
        // 5. Tạo và trả về response
        return buildPaymentResponse(payment);
    }


    private void sendPaymentNotification(Payment payment) {

        var currencyFormatter = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));
        String formattedAmount = currencyFormatter.format(payment.getAmount());
        String notificationTitle = "Thanh toán thành công #%s".formatted(payment.getOrderCode());

        String notificationContent = """
                Bạn đã thanh toán thành công %s để xem danh sách ứng viên của tin tuyển dụng "%s". \
                Mã giao dịch: %s. Cảm ơn bạn đã sử dụng dịch vụ!\
                """.formatted(formattedAmount, payment.getJob().getName(), payment.getTransactionId());
        // . Gọi notification service
        NotificationRequest request=NotificationRequest.builder()
                .user(payment.getUser()).title(notificationTitle)
                .content(notificationContent).type(NotificationType.PAYMENT)
                .referenceId(payment.getId())
                .redirectUrl("www.webJob.vn/api/v1/jobs/"+payment.getJob().getId())
                .build();
        notificationService.createNotification(
                request
        );
    }


    public JobApplicantInfoResponse getJobApplicantInfo(Long userId, Long jobId) {
        // Kiểm tra job tồn tại
        Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy công việc"));

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

        boolean locked = redisLockService.tryLock(lockKey, lockValue, 2, TimeUnit.MINUTES);
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

            validatePaymentForJob(userId,job);

            Payment payment = createMomoPayment(user, job, request);
            String paymentUrl = momoService.createPaymentRequest(
                    COMPETITION_VIEW_PRICE.toString()
                    ,payment.getOrderCode()
            );
            log.info("Payment created | userId: {}, jobId: {}, paymentId: {}",
                    userId, job.getId(), payment.getId()
            );
            return buildPaymentResponse(
                    payment,userId,job,paymentUrl
            );

        } finally {
            // Luôn unlock dù success hay exception
            redisLockService.unlock(lockKey, lockValue);

        }

    }
    private Payment createMomoPayment(User user, Job job, PaymentCreateRequest request) {
        String orderInfo = String.format(
                "Thanh toán xem thông tin ứng viên - Công việc: %s",
                job.getName()
        );

        String requestId = "MOMO" + new Date().getTime();

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

        return paymentRepository.save(payment);
    }
    @Transactional
    public PaymentResponse handleMomoReturn(MomoPaymentCallback callback) {

        return handlePaymentMomo(callback);
    }

    public PaymentResponse handlePaymentMomo(MomoPaymentCallback callbackRequest) {
        // 1. Validate chữ ký
        boolean validationResult = momoService.verifyMomoCallbackSignature(callbackRequest);
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
            payment.setStatus(PaymentStatus.EXPIRED.name());
            paymentRepository.save(payment);
            throw new PaymentExpiredException("Payment expired");
        }


        // 4. Cập nhật trạng thái thanh toán
        if ("0".equals(callbackRequest.getResultCode())) {
            payment.setStatus(PaymentStatus.SUCCESS.name());
        } else {
            payment.setStatus(PaymentStatus.FAILED.name());
        }
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
        log.info("Payment {} for transactionRef: {}", payment.getStatus(), callbackRequest.getResultCode());
        if (payment.getStatus().equals(PaymentStatus.SUCCESS.name())) {
            eventPublisher.publishEvent(
                    new PaymentSuccessEvent(payment.getId())

            );
            sendPaymentNotification(payment);
        }
        // 5. Tạo và trả về response
        return buildPaymentResponse(payment);
    }

    public List<PaymentResponse> getPaymentHistory() {
        // Lấy user ID từ authentication (giả sử có getUserId method)
        User user = securityUtils.getCurrentUser();
        return getUserPaymentHistory(user.getId());
    }

}
