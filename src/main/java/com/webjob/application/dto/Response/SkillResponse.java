package com.webjob.application.dto.Response;

import com.webjob.application.enums.SkillAliasType;
import com.webjob.application.enums.SkillStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.w3c.dom.stylesheets.LinkStyle;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class SkillResponse {
    private Long id;

    private String name;

    private String description;

    private SkillStatus status;

    private Instant createdAt;

    private String createdBy;
    List<SkillAliasResponse> skillAliasResponses;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillAliasResponse{
        private String alias;


        private SkillAliasType aliasType;

        private String normalizedAlias;

        private SkillStatus status = SkillStatus.ACTIVE;

    }





}
