package com.webjob.application.enums;

public enum ResumeStatus {
    PENDING,    // Mới nộp, chờ xử lý
    REVIEWING,  // Đang xem xét CV
    INTERVIEWING,// Đang trong các vòng phỏng vấn
    OFFERED,     // Đã gửi offer mời nhận việc
    HIRED,   // Đã chốt ((đã tuyển đồng ý đi làm)
    REJECTED    // Bị từ chối / Bị loại
}

//PENDING
// ├──→ REVIEWING
// └──→ REJECTED
//
//        REVIEWING
// ├──→ INTERVIEWING
// └──→ REJECTED
//
//        INTERVIEWING
// ├──→ OFFERED
// └──→ REJECTED
//
//        OFFERED
// ├──→ APPROVED
// └──→ REJECTED
//
//APPROVED (kết thúc)
//REJECTED (kết thúc)
