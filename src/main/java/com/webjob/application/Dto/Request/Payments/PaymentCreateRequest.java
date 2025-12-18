package com.webjob.application.Dto.Request.Payments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {
    @NotNull(message = "Job ID không được để trống")
    private Long jobId;
    @NotBlank(message = "Phải chọn cổng thanh toán")
    @Pattern(regexp = "VNPAY|MOMO|ZALOPAY", message = "Giá trị hợp lệ: VNPAY, MOMO, ZALOPAY")
    private String gateway;  // Tên cổng thanh toán

}
