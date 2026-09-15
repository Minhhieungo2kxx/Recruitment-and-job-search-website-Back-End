package com.webjob.application.dto.Request;

import com.webjob.application.enums.SkillAliasType;
import com.webjob.application.enums.SkillStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SkillRequest {

    @NotBlank(message = "Tên kỹ năng không được để trống")
    @Size(
            max = 100,
            message = "Tên kỹ năng không được vượt quá 100 ký tự"
    )
    private String name;

    @Size(
            max = 1000,
            message = "Mô tả không được vượt quá 1000 ký tự"
    )
    private String description;

    @NotNull(message = "Trạng thái không được để trống")
    private SkillStatus status = SkillStatus.ACTIVE;

    /**
     * Các alias dùng để nhận diện skill trong CV.
     */
    @Valid
    private List<SkillAliasRequest> aliases = new ArrayList<>();


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillAliasRequest{

        /**
         * Alias có thể xuất hiện trong CV.
         *
         * Ví dụ:
         * K8s
         * ReactJS
         * React.js
         */
        @NotBlank(message = "Alias không được để trống")
        @Size(
                max = 100,
                message = "Alias không được vượt quá 100 ký tự"
        )
        private String alias;

        /**
         * ALIAS
         * ABBREVIATION
         * VARIANT
         */
        @NotNull(message = "Loại alias không được để trống")
        private SkillAliasType aliasType = SkillAliasType.ALIAS;

        /**
         * ACTIVE / INACTIVE
         */
        @NotNull(message = "Trạng thái alias không được để trống")
        private SkillStatus status = SkillStatus.ACTIVE;

    }
}
