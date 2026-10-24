package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.NotificationRequest;
import com.webjob.application.dto.Request.Payments.MomoPaymentCallback;
import com.webjob.application.dto.Request.Payments.PaymentCallbackRequest;
import com.webjob.application.dto.Request.Payments.PaymentCreateRequest;
import com.webjob.application.dto.Response.JobApplicantInfoResponse;
import com.webjob.application.dto.Response.PaymentResponse;
import com.webjob.application.dto.record.PaymentSuccessEvent;
import com.webjob.application.enums.CompetitionLevel;
import com.webjob.application.enums.PaymentStatus;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.BusinessException;
import com.webjob.application.exception.Customs.PaymentExpiredException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.Payment;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.PaymentRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.PaymentGateway.MomoService;
import com.webjob.application.service.PaymentGateway.VNPayService;
import com.webjob.application.service.Redis.RedisLockService;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
public class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VNPayService vnPayService;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEmailService applicationEmailService;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private MomoService momoService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Job job;
    private Payment payment;
    private PaymentCreateRequest createRequest;

    private static final Long USER_ID = 1L;
    private static final Long JOB_ID = 10L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long AMOUNT = 30_000L;
    private static final String ORDER_CODE = "ORDER123";
    private static final String LOCK_KEY = "payment:create:1:10";
    private static final String LOCK_VALUE = "LOCK1234";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        job = new Job();
        job.setId(JOB_ID);
        job.setName("Java Developer");
        job.setCompetitionLevel(CompetitionLevel.HIGH);

        createRequest = new PaymentCreateRequest();
        createRequest.setJobId(JOB_ID);
        createRequest.setGateway("VNPAY");

        payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setUser(user);
        payment.setJob(job);
        payment.setAmount(AMOUNT);
        payment.setStatus(PaymentStatus.PENDING.name());
        payment.setOrderCode(ORDER_CODE);
        payment.setProvider("VNPAY");
        payment.setOrderInfo("Thanh toán xem thông tin ứng viên");
        payment.setExpiredAt(LocalDateTime.now().plusMinutes(10));

        lenient().when(redisLockService.tryLock(
                eq(LOCK_KEY),
                anyString(),
                anyLong(),
                any(TimeUnit.class)
        )).thenReturn(true);

        lenient().when(jobRepository.findById(JOB_ID))
                .thenReturn(Optional.of(job));

        lenient().when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        lenient().when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment saved = invocation.getArgument(0);
                    if (saved.getId() == null) {
                        saved.setId(PAYMENT_ID);
                    }
                    return saved;
                });
    }

    // =========================================================
    // 1. CREATE PAYMENT
    // =========================================================

    @Nested
    class CreatePaymentTests {

        @Test
        @DisplayName("Tạo thanh toán VNPay thành công")
        void createPaymentVnpay_success() {
            when(paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    eq(USER_ID),
                    eq(JOB_ID),
                    eq("PENDING"),
                    any(LocalDateTime.class)
            )).thenReturn(false);

            when(paymentRepository.existsByUserIdAndJobIdAndStatus(
                    USER_ID, JOB_ID, "SUCCESS"
            )).thenReturn(false);

            when(vnPayService.generateTxnRef(USER_ID)).thenReturn(ORDER_CODE);

            when(vnPayService.createOrder(
                    any(HttpServletRequest.class),
                    anyMap()
            )).thenReturn("https://sandbox.vnpayment.vn/payment");

            PaymentResponse response = paymentService.createPaymentVnpay(
                    USER_ID, createRequest, mock(HttpServletRequest.class)
            );

            assertEquals(PAYMENT_ID, response.getId());
            assertEquals(USER_ID, response.getUserId());
            assertEquals(JOB_ID, response.getJobId());
            assertEquals(AMOUNT, response.getAmount());
            assertEquals("PENDING", response.getStatus());
            assertEquals(
                    "https://sandbox.vnpayment.vn/payment",
                    response.getPaymentUrl()
            );


            verify(paymentRepository).save(any(Payment.class));
            verify(redisLockService).unlock(
                    eq(LOCK_KEY),
                    anyString()
            );
        }

        @Test
        @DisplayName("Không tạo thanh toán khi không lấy được Redis lock")
        void createPaymentVnpay_lockUnavailable_throwsException() {
            when(redisLockService.tryLock(
                    anyString(),
                    anyString(),
                    anyLong(),
                    any(TimeUnit.class)
            )).thenReturn(false);

            assertThrows(
                    BusinessException.class,
                    () -> paymentService.createPaymentVnpay(
                            USER_ID, createRequest, mock(HttpServletRequest.class)
                    )
            );

            verifyNoInteractions(jobRepository, userRepository, vnPayService);
            verify(paymentRepository, never()).save(any());
            verify(redisLockService, never()).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("Không tạo thanh toán khi job không tồn tại")
        void createPaymentVnpay_jobNotFound_throwsException() {

            when(jobRepository.findById(JOB_ID))
                    .thenReturn(Optional.empty());

            assertThrows(
                    BusinessException.class,
                    () -> paymentService.createPaymentVnpay(
                            USER_ID, createRequest, mock(HttpServletRequest.class)
                    )
            );

            verify(redisLockService).unlock(eq(LOCK_KEY),anyString());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Không tạo thanh toán cho job không thuộc mức HIGH")
        void createPaymentVnpay_jobNotHigh_throwsException() {
            job.setCompetitionLevel(CompetitionLevel.LOW);

            assertThrows(
                    BusinessException.class,
                    () -> paymentService.createPaymentVnpay(
                            USER_ID, createRequest, mock(HttpServletRequest.class)
                    )
            );

            verify(paymentRepository, never()).save(any());
            verify(redisLockService).unlock(eq(LOCK_KEY), anyString());
        }

        @Test
        @DisplayName("Không tạo thanh toán nếu đã có giao dịch PENDING")
        void createPaymentVnpay_pendingPaymentExists_throwsException() {
            when(paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    eq(USER_ID),
                    eq(JOB_ID),
                    eq("PENDING"),
                    any(LocalDateTime.class)
            )).thenReturn(true);

            assertThrows(
                    BusinessException.class,
                    () -> paymentService.createPaymentVnpay(
                            USER_ID, createRequest, mock(HttpServletRequest.class)
                    )
            );

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Không tạo thanh toán nếu người dùng đã trả phí")
        void createPaymentVnpay_alreadyPaid_throwsException() {
            when(paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    eq(USER_ID),
                    eq(JOB_ID),
                    eq("PENDING"),
                    any(LocalDateTime.class)
            )).thenReturn(false);

            when(paymentRepository.existsByUserIdAndJobIdAndStatus(
                    USER_ID, JOB_ID, "SUCCESS"
            )).thenReturn(true);

            assertThrows(
                    BusinessException.class,
                    () -> paymentService.createPaymentVnpay(
                            USER_ID, createRequest, mock(HttpServletRequest.class)
                    )
            );

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Luôn giải phóng Redis lock khi tạo thanh toán gặp lỗi")
        void createPaymentVnpay_gatewayThrows_unlocksLock() {
            when(paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    eq(USER_ID),
                    eq(JOB_ID),
                    eq("PENDING"),
                    any(LocalDateTime.class)
            )).thenReturn(false);

            when(paymentRepository.existsByUserIdAndJobIdAndStatus(
                    USER_ID, JOB_ID, "SUCCESS"
            )).thenReturn(false);

            when(vnPayService.generateTxnRef(USER_ID))
                    .thenReturn(ORDER_CODE);

            when(vnPayService.createOrder(any(), anyMap()))
                    .thenThrow(new RuntimeException("Gateway unavailable"));

            assertThrows(
                    RuntimeException.class,
                    () -> paymentService.createPaymentVnpay(
                            USER_ID, createRequest, mock(HttpServletRequest.class)
                    )
            );

            verify(redisLockService).unlock(eq(LOCK_KEY),anyString());
        }

        @Test
        @DisplayName("Tạo thanh toán MoMo thành công")
        void createPaymentMomo_success() {
            createRequest.setGateway("MOMO");

            when(paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    eq(USER_ID),
                    eq(JOB_ID),
                    eq("PENDING"),
                    any(LocalDateTime.class)
            )).thenReturn(false);

            when(paymentRepository.existsByUserIdAndJobIdAndStatus(
                    USER_ID, JOB_ID, "SUCCESS"
            )).thenReturn(false);

            when(momoService.createPaymentRequest(
                    eq(AMOUNT.toString()), anyString()
            )).thenReturn("https://momo.vn/payment");

            PaymentResponse response = paymentService.createPaymentMomo(
                    USER_ID, createRequest, mock(HttpServletRequest.class)
            );

            assertEquals(AMOUNT, response.getAmount());
            assertEquals("PENDING", response.getStatus());
            assertEquals("https://momo.vn/payment", response.getPaymentUrl());

            verify(paymentRepository).save(any(Payment.class));
            verify(redisLockService).unlock(eq(LOCK_KEY),anyString());
        }

        @Test
        @DisplayName("Giải phóng Redis lock nếu MoMo không tạo được yêu cầu thanh toán")
        void createPaymentMomo_gatewayThrows_unlocksLock() {
            when(paymentRepository.existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(
                    eq(USER_ID),
                    eq(JOB_ID),
                    eq("PENDING"),
                    any(LocalDateTime.class)
            )).thenReturn(false);

            when(paymentRepository.existsByUserIdAndJobIdAndStatus(
                    USER_ID, JOB_ID, "SUCCESS"
            )).thenReturn(false);

            when(momoService.createPaymentRequest(anyString(), anyString()))
                    .thenThrow(new RuntimeException("MoMo unavailable"));

            assertThrows(
                    RuntimeException.class,
                    () -> paymentService.createPaymentMomo(
                            USER_ID, createRequest, mock(HttpServletRequest.class)
                    )
            );

            verify(redisLockService).unlock(eq(LOCK_KEY),anyString());
        }
    }
    // =========================================================
    // 2. VNPAY CALLBACK
    // =========================================================

    @Nested
    class VnPayCallbackTests {

        private PaymentCallbackRequest callback;

        @BeforeEach
        void setUpCallback() {
            callback = new PaymentCallbackRequest();
            callback.setVnp_TxnRef(ORDER_CODE);
            callback.setVnp_Amount("3000000");
            callback.setVnp_ResponseCode("00");
            callback.setVnp_TransactionStatus("00");
            callback.setVnp_TransactionNo("VNP123");
            callback.setVnp_BankCode("NCB");
            callback.setVnp_SecureHash("valid-signature");
            callback.setVnp_PayDate("20261020120000");
        }

        @Test
        @DisplayName("Callback VNPay hợp lệ và thanh toán thành công")
        void handlePaymentCallbackVnPay_success() {
            when(vnPayService.orderReturn(any(HttpServletRequest.class)))
                    .thenReturn(1);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.handlePaymentCallbackVnPay(
                    callback, mock(HttpServletRequest.class)
            );

            assertEquals("SUCCESS", response.getStatus());
            assertEquals("VNP123", response.getTransactionId());
            assertEquals("NCB", response.getBankCode());

            verify(paymentRepository).save(payment);
            verify(applicationEmailService).sendPaymentEmail(payment);
            verify(notificationService).createNotification(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("Từ chối callback VNPay có chữ ký không hợp lệ")
        void handlePaymentCallbackVnPay_invalidSignature_throwsException() {
            when(vnPayService.orderReturn(any(HttpServletRequest.class)))
                    .thenReturn(0);

            assertThrows(
                    BadRequestException.class,
                    () -> paymentService.handlePaymentCallbackVnPay(
                            callback, mock(HttpServletRequest.class)
                    )
            );

            verify(paymentRepository, never()).findByOrderCode(anyString());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Callback VNPay không tìm thấy payment")
        void handlePaymentCallbackVnPay_paymentNotFound_throwsException() {
            when(vnPayService.orderReturn(any(HttpServletRequest.class)))
                    .thenReturn(1);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> paymentService.handlePaymentCallbackVnPay(
                            callback, mock(HttpServletRequest.class)
                    )
            );

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Từ chối callback VNPay có số tiền không khớp")
        void handlePaymentCallbackVnPay_amountMismatch_throwsException() {
            callback.setVnp_Amount("2000000");

            when(vnPayService.orderReturn(any(HttpServletRequest.class)))
                    .thenReturn(1);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            assertThrows(
                    RuntimeException.class,
                    () -> paymentService.handlePaymentCallbackVnPay(
                            callback, mock(HttpServletRequest.class)
                    )
            );

            verify(paymentRepository, never()).save(any());
            verify(applicationEmailService, never()).sendPaymentEmail(any());
        }

        @Test
        @DisplayName("Callback VNPay hết hạn không được cấp quyền xem")
        void handlePaymentCallbackVnPay_expired_throwsException() {
            payment.setExpiredAt(LocalDateTime.now().minusMinutes(1));

            when(vnPayService.orderReturn(any(HttpServletRequest.class)))
                    .thenReturn(1);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            assertThrows(
                    PaymentExpiredException.class,
                    () -> paymentService.handlePaymentCallbackVnPay(
                            callback, mock(HttpServletRequest.class)
                    )
            );

            assertEquals("EXPIRED", payment.getStatus());
            verify(paymentRepository).save(payment);
            verify(applicationEmailService, never()).sendPaymentEmail(any());
            verify(notificationService, never())
                    .createNotification(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("Callback VNPay thất bại không gửi email hoặc thông báo thành công")
        void handlePaymentCallbackVnPay_failed_doesNotNotifySuccess() {
            callback.setVnp_ResponseCode("24");
            callback.setVnp_TransactionStatus("24");

            when(vnPayService.orderReturn(any(HttpServletRequest.class)))
                    .thenReturn(1);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.handlePaymentCallbackVnPay(
                    callback, mock(HttpServletRequest.class)
            );

            assertEquals("FAILED", response.getStatus());
            verify(paymentRepository).save(payment);
            verify(applicationEmailService, never()).sendPaymentEmail(any());
            verify(notificationService, never())
                    .createNotification(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("Callback VNPay lặp lại không xử lý thanh toán lần nữa")
        void handlePaymentCallbackVnPay_duplicateSuccess_isIdempotent() {
            payment.setStatus("SUCCESS");

            when(vnPayService.orderReturn(any(HttpServletRequest.class)))
                    .thenReturn(1);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.handlePaymentCallbackVnPay(
                    callback, mock(HttpServletRequest.class)
            );

            assertEquals("SUCCESS", response.getStatus());

            verify(paymentRepository, never()).save(any());
            verify(applicationEmailService, never()).sendPaymentEmail(any());
            verify(notificationService, never())
                    .createNotification(any(NotificationRequest.class));
        }
    }

    // =========================================================
    // 3. MOMO CALLBACK
    // =========================================================

    @Nested
    class MomoCallbackTests {

        private MomoPaymentCallback callback;

        @BeforeEach
        void setUpCallback() {
            callback = new MomoPaymentCallback();
            callback.setOrderId(ORDER_CODE);
            callback.setAmount("30000");
            callback.setResultCode("0");
            callback.setTransId("MOMO123");
            callback.setSignature("valid-signature");
            callback.setResponseTime(String.valueOf(
                    System.currentTimeMillis()
            ));
            callback.setOrderType("momo_wallet");
            callback.setPayType("qr");
        }

        @Test
        @DisplayName("Callback MoMo hợp lệ và thanh toán thành công")
        void handlePaymentMomo_success() {
            when(momoService.verifyMomoCallbackSignature(callback))
                    .thenReturn(true);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.handlePaymentMomo(callback);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals("MOMO123", response.getTransactionId());

            verify(paymentRepository).save(payment);

//            verify(eventPublisher).publishEvent(
//                    argThat(event -> event instanceof PaymentSuccessEvent)
//            );
            verify(notificationService).createNotification(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("Từ chối callback MoMo có chữ ký không hợp lệ")
        void handlePaymentMomo_invalidSignature_throwsException() {
            when(momoService.verifyMomoCallbackSignature(callback))
                    .thenReturn(false);

            assertThrows(
                    RuntimeException.class,
                    () -> paymentService.handlePaymentMomo(callback)
            );

            verify(paymentRepository, never()).findByOrderCode(anyString());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Từ chối callback MoMo có số tiền không khớp")
        void handlePaymentMomo_amountMismatch_throwsException() {
            callback.setAmount("20000");

            when(momoService.verifyMomoCallbackSignature(callback))
                    .thenReturn(true);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            assertThrows(
                    RuntimeException.class,
                    () -> paymentService.handlePaymentMomo(callback)
            );

            verify(paymentRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Callback MoMo hết hạn không phát sự kiện thanh toán thành công")
        void handlePaymentMomo_expired_throwsException() {
            payment.setExpiredAt(LocalDateTime.now().minusMinutes(1));

            when(momoService.verifyMomoCallbackSignature(callback))
                    .thenReturn(true);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            assertThrows(
                    PaymentExpiredException.class,
                    () -> paymentService.handlePaymentMomo(callback)
            );

            assertEquals("EXPIRED", payment.getStatus());
            verify(paymentRepository).save(payment);
            verify(eventPublisher, never()).publishEvent(any());
            verify(notificationService, never())
                    .createNotification(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("Callback MoMo thất bại không phát sự kiện thành công")
        void handlePaymentMomo_failed_doesNotPublishSuccessEvent() {
            callback.setResultCode("1006");

            when(momoService.verifyMomoCallbackSignature(callback))
                    .thenReturn(true);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.handlePaymentMomo(callback);

            assertEquals("FAILED", response.getStatus());
            verify(paymentRepository).save(payment);
            verify(eventPublisher, never()).publishEvent(any());
            verify(notificationService, never())
                    .createNotification(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("Callback MoMo lặp lại không phát sự kiện lần nữa")
        void handlePaymentMomo_duplicateSuccess_isIdempotent() {
            payment.setStatus("SUCCESS");

            when(momoService.verifyMomoCallbackSignature(callback))
                    .thenReturn(true);

            when(paymentRepository.findByOrderCode(ORDER_CODE))
                    .thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.handlePaymentMomo(callback);

            assertEquals("SUCCESS", response.getStatus());

            verify(paymentRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
            verify(notificationService, never())
                    .createNotification(any(NotificationRequest.class));
        }
    }

    // =========================================================
    // 4. JOB APPLICANT ACCESS
    // =========================================================

    @Nested
    class JobApplicantInfoTests {

        @Test
        @DisplayName("Job mức HIGH chưa thanh toán thì ẩn số lượng ứng viên")
        void getJobApplicantInfo_highCompetitionNotPaid_hidesCount() {
            when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                    .thenReturn(Optional.of(job));

            when(paymentRepository.existsByUserIdAndJobIdAndStatus(
                    USER_ID, JOB_ID, "SUCCESS"
            )).thenReturn(false);

            JobApplicantInfoResponse response =
                    paymentService.getJobApplicantInfo(USER_ID, JOB_ID);

            assertEquals(JOB_ID, response.getJobId());
            assertEquals(-1, response.getAppliedCount());
            assertFalse(response.isHasPaidAccess());
            assertTrue(response.getMessage().contains("30000"));
        }

        @Test
        @DisplayName("Job mức HIGH đã thanh toán thì được xem số lượng ứng viên")
        void getJobApplicantInfo_highCompetitionPaid_showsCount() {
            job.setAppliedCount(25);

            when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                    .thenReturn(Optional.of(job));

            when(paymentRepository.existsByUserIdAndJobIdAndStatus(
                    USER_ID, JOB_ID, "SUCCESS"
            )).thenReturn(true);

            JobApplicantInfoResponse response =
                    paymentService.getJobApplicantInfo(USER_ID, JOB_ID);

            assertEquals(25, response.getAppliedCount());
            assertTrue(response.isHasPaidAccess());
        }

        @Test
        @DisplayName("Job không thuộc mức HIGH thì xem miễn phí")
        void getJobApplicantInfo_freeCompetition_allowsAccess() {
            job.setCompetitionLevel(CompetitionLevel.LOW);
            job.setAppliedCount(12);

            when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                    .thenReturn(Optional.of(job));

            JobApplicantInfoResponse response =
                    paymentService.getJobApplicantInfo(USER_ID, JOB_ID);

            assertEquals(12, response.getAppliedCount());
            assertTrue(response.isHasPaidAccess());

            verify(paymentRepository, never())
                    .existsByUserIdAndJobIdAndStatus(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("Không tìm thấy job thì báo lỗi")
        void getJobApplicantInfo_jobNotFound_throwsException() {
            when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                    .thenReturn(Optional.empty());

            assertThrows(
                    BadRequestException.class,
                    () -> paymentService.getJobApplicantInfo(USER_ID, JOB_ID)
            );
        }
    }

    // =========================================================
    // 5. PAYMENT HISTORY
    // =========================================================

    @Nested
    class PaymentHistoryTests {

        @Test
        @DisplayName("Lịch sử thanh toán chỉ ánh xạ các giao dịch thành công")
        void getUserPaymentHistory_returnsSuccessfulPayments() {
            Payment successfulPayment = new Payment();
            successfulPayment.setId(PAYMENT_ID);
            successfulPayment.setUser(user);
            successfulPayment.setJob(job);
            successfulPayment.setAmount(AMOUNT);
            successfulPayment.setStatus("SUCCESS");
            successfulPayment.setTransactionId("TXN123");

            when(paymentRepository.findByUserIdAndStatus(USER_ID, "SUCCESS"))
                    .thenReturn(List.of(successfulPayment));

            List<PaymentResponse> responses =
                    paymentService.getUserPaymentHistory(USER_ID);

            assertEquals(1, responses.size());

            PaymentResponse response = responses.get(0);
            assertEquals(PAYMENT_ID, response.getId());
            assertEquals(USER_ID, response.getUserId());
            assertEquals(JOB_ID, response.getJobId());
            assertEquals(AMOUNT, response.getAmount());
            assertEquals("SUCCESS", response.getStatus());
            assertEquals("TXN123", response.getTransactionId());

            verify(paymentRepository).findByUserIdAndStatus(USER_ID, "SUCCESS");
        }

        @Test
        @DisplayName("Lịch sử thanh toán rỗng khi người dùng chưa có giao dịch thành công")
        void getUserPaymentHistory_noSuccessfulPayments_returnsEmptyList() {
            when(paymentRepository.findByUserIdAndStatus(USER_ID, "SUCCESS"))
                    .thenReturn(Collections.emptyList());

            List<PaymentResponse> responses =
                    paymentService.getUserPaymentHistory(USER_ID);

            assertTrue(responses.isEmpty());
        }

        @Test
        @DisplayName("Lấy lịch sử thanh toán theo người dùng hiện tại")
        void getPaymentHistory_usesAuthenticatedUser() {
            when(securityUtils.getCurrentUser()).thenReturn(user);

            when(paymentRepository.findByUserIdAndStatus(USER_ID, "SUCCESS"))
                    .thenReturn(Collections.emptyList());

            List<PaymentResponse> responses = paymentService.getPaymentHistory();

            assertTrue(responses.isEmpty());
            verify(paymentRepository).findByUserIdAndStatus(USER_ID, "SUCCESS");
        }
    }




}
