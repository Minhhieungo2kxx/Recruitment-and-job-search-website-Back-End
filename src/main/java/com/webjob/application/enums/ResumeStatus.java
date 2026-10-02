package com.webjob.application.enums;

public enum ResumeStatus {
    PENDING,        // Mới nộp, chờ xử lý
    REVIEWING,      // Recruiter đang xem xét
    SHORTLISTED,    // Được chọn vào danh sách phù hợp
    INTERVIEWING,   // Đang phỏng vấn
    OFFERED,        // Đã gửi offer
    HIRED,          // Đã nhận việc / chốt tuyển
    REJECTED,       // Bị từ chối
    WITHDRAWN       // Ứng viên chủ động rút hồ sơ
}

//Status	Matchable?	Lý do
//PENDING		CV mới nộp, vẫn cần matching
//REVIEWING		Recruiter đang xem xét
//SHORTLISTED		Đã lọt shortlist, vẫn còn trong pipeline
//INTERVIEWING		Đang phỏng vấn, chưa kết thúc
//OFFERED		Đã offer nhưng chưa hired
//HIRED		Đã tuyển xong
//REJECTED		Đã bị loại
//WITHDRAWN		Ứng viên đã rút


