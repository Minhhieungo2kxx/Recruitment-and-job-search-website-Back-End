package com.webjob.application.Model.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)

public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @NotNull(message = "Số tiền không được để trống")
    @PositiveOrZero(message = "Số tiền phải là số không âm")
    private Long amount;

    @NotBlank(message = "Trạng thái thanh toán không được để trống")
    @Pattern(
            regexp = "PENDING|SUCCESS|FAILED|EXPIRED",
            message = "Trạng thái không hợp lệ. Giá trị hợp lệ: PENDING, SUCCESS, FAILED,EXPIRED"
    )
    @Column(length = 20)
    private String status; // PENDING, SUCCESS, FAILED

    @Size(max = 100)
    private String transactionId; // Mã giao dịch chung

    @Size(max = 100)
    private String orderCode;

    @Column(length = 20)
    private String provider; // VNPAY, MOMO, ZALOPAY

    @Column(length = 255)
    private String paymentGatewayResponse; // Thông tin phản hồi từ cổng thanh toán

    @Column(length = 20)
    private String bankCode; // Mã ngân hàng, nếu có (Ví dụ: VNPAY có thể có mã ngân hàng)

    private Instant payDate; // Ngày giờ thanh toán

    @Column(length = 255)
    private String secureHash; // Chữ ký bảo mật từ các cổng thanh toán

    private String responseCode; // Mã phản hồi từ cổng thanh toán (Ví dụ: vnp_ResponseCode, momo_responseCode)

    private String orderInfo; // Thông tin chi tiết về đơn hàng

    @Column(length = 255)
    private String payType;

    @Column(length = 255)
    private String orderType;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant createdAt;

    @Column(name = "created_by")
    @Size(max = 100)
    @CreatedBy
    private String createdBy;
    private LocalDateTime expiredAt;


}

//CREATE UNIQUE INDEX ux_payment_pending
//ON payment(user_id, job_id)
//WHERE status = 'PENDING';
