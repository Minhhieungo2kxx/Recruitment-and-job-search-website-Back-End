package com.webjob.application.enums;

/** Kết quả so khớp 1 JobSkill với CandidateSkill */
public enum MatchType {
    MATCHED,        // đủ skill + đủ điều kiện năm kinh nghiệm/level
    PARTIAL,        // có skill nhưng thiếu năm kinh nghiệm/level yêu cầu
    MISSING         // không có skill này trong CV
}
