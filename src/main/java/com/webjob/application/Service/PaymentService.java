package com.webjob.application.Service;

import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Model.Entity.Payment;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Model.Enums.CompetitionLevel;
import com.webjob.application.Model.Enums.PaymentStatus;
import com.webjob.application.Dto.Request.PaymentCallbackRequest;
import com.webjob.application.Dto.Request.PaymentCreateRequest;
import com.webjob.application.Dto.Response.JobApplicantInfoResponse;
import com.webjob.application.Dto.Response.PaymentResponse;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.PaymentRepository;
import com.webjob.application.Repository.UserRepository;
import com.webjob.application.Service.SendEmail.ApplicationEmailService;
import com.webjob.application.Service.VnPay.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final VNPayService vnPayService;

    private final UserService userService;
    private final ApplicationEmailService applicationEmailService;

    // Giá cố định để xem thông tin ứng viên
    private static final Long COMPETITION_VIEW_PRICE =20000L; // 20,000 VND

    public PaymentService(PaymentRepository paymentRepository,
                          JobRepository jobRepository,
                          UserRepository userRepository,
                          VNPayService vnPayService, UserService userService, ApplicationEmailService applicationEmailService) {
        this.paymentRepository = paymentRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.vnPayService = vnPayService;
        this.userService = userService;
        this.applicationEmailService = applicationEmailService;
    }

    public PaymentResponse createPaymentForJobView(Long userId, PaymentCreateRequest request, HttpServletRequest httpRequest) {
        // Lấy job từ DB, nếu không tồn tại thì báo lỗi
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        // Lấy user từ DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra mức độ cạnh tranh của công việc
        if (job.getCompetitionLevel() != CompetitionLevel.HIGH) {
            throw new RuntimeException("Công việc này không yêu cầu thanh toán để xem thông tin ứng viên");
        }
        boolean hasPending = paymentRepository.existsByUserIdAndJobIdAndStatus(
                userId, job.getId(), PaymentStatus.PENDING.name()
        );
        if (hasPending) {
            throw new RuntimeException("Bạn đang có giao dịch đang xử lý cho công việc này. Vui lòng chờ.");
        }
        // Kiểm tra xem user đã thanh toán thành công cho job này chưa
        boolean hasPaid = paymentRepository.existsByUserIdAndJobIdAndStatus(userId, job.getId(), PaymentStatus.SUCCESS.name());
        if (hasPaid) {
            throw new RuntimeException("Bạn đã thanh toán để xem thông tin ứng viên của công việc này");
        }
        // Tạo link thanh toán VNPay
        String orderInfo = String.format("Thanh toán xem thông tin ứng viên - Công việc: %s", job.getName());
        // Tạo bản ghi thanh toán
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setJob(job);
        payment.setAmount(COMPETITION_VIEW_PRICE);
        payment.setStatus(PaymentStatus.PENDING.name());
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
    }


    @Transactional
    public PaymentResponse handlePaymentCallback(PaymentCallbackRequest callbackRequest, HttpServletRequest request) {
        // 1. Validate chữ ký từ VNPay
        int validationResult = vnPayService.orderReturn(request);
        if (validationResult == -1) {
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

        // 4. Cập nhật trạng thái thanh toán
        String status = (validationResult == 1)
                ? PaymentStatus.SUCCESS.name()
                : PaymentStatus.FAILED.name();

        payment.setStatus(status);
        payment.setTransactionId(callbackRequest.getVnp_TransactionNo()); // Mã giao dịch thật từ VNPay
        payment.setBankCode(callbackRequest.getVnp_BankCode());
        payment.setSecureHash(callbackRequest.getVnp_SecureHash());
        String payDateStr = callbackRequest.getVnp_PayDate(); // "20251203143025"

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh")); // múi giờ VN
        Instant payDate = LocalDateTime.parse(payDateStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toInstant();
        payment.setPayDate(payDate);
        payment.setPaymentGatewayResponse("ResponseCode: " + callbackRequest.getVnp_ResponseCode() +
                ", TransactionStatus: " + callbackRequest.getVnp_TransactionStatus());
        payment.setResponseCode(callbackRequest.getVnp_ResponseCode());
        payment = paymentRepository.save(payment);

        log.info("Payment {} for transactionRef: {}", status.toLowerCase(),callbackRequest.getVnp_TransactionNo());
        if(payment.getStatus().equals("SUCCESS")){
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

        return response;
    }










}
