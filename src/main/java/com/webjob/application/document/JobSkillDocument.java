package com.webjob.application.document;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSkillDocument {
    private Long id;

    private Long skillId;

    private String skillName;
}
