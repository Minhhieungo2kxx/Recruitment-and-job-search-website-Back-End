package com.webjob.application.dto.Request;

import com.webjob.application.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ParsedSkill {
    private String name;
    private Double yearsOfExperience;
    private SkillLevel level;
}
