package com.webjob.application.enums;

/** Nguồn gốc việc chuẩn hoá 1 CandidateSkill về Skill chuẩn trong hệ thống */
public enum SkillSource {
    MATCHED_BY_NAME,
    MATCHED_BY_ALIAS,  // khớp qua SkillAlias.normalizedAlias (rule-based)
    MATCHED_BY_AI,     // AI suy luận semantic (vd "Node" -> "Node.js")
    UNMATCHED          // không map được vào Skill chuẩn nào
}
