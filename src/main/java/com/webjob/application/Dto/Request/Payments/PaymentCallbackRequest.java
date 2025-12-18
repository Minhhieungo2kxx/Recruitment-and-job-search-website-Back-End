package com.webjob.application.Dto.Request.Payments;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    private String vnp_Amount;
    private String vnp_BankCode;
    private String vnp_BankTranNo;
    private String vnp_CardType;
    private String vnp_OrderInfo;
    private String vnp_PayDate;
    private String vnp_ResponseCode;
    private String vnp_TmnCode;
    private String vnp_TransactionNo;
    private String vnp_TransactionStatus;
    private String vnp_TxnRef;
    private String vnp_SecureHash;
}

//| Trường                    | Ý nghĩa                         |
//        | ------------------------- | ------------------------------- |
//        | **vnp_TxnRef**            | Mã đơn của bạn                  |
//        | **vnp_TransactionNo**     | Mã giao dịch VNPay              |
//        | **vnp_Amount**            | Số tiền ×100                    |
//        | **vnp_ResponseCode**      | Trạng thái VNPay                |
//        | **vnp_TransactionStatus** | Trạng thái ngân hàng            |
//        | **vnp_PayDate**           | Thời gian thanh toán thành công |
//        | **vnp_SecureHash**        | Chữ ký xác thực                 |
//        | vnp_BankCode              | Mã ngân hàng                    |
//        | vnp_BankTranNo            | Mã giao dịch ngân hàng          |
//        | vnp_CardType              | Loại thẻ                        |
//        | vnp_OrderInfo             | Nội dung thanh toán             |
//        | vnp_TmnCode               | Merchant ID của bạn             |

