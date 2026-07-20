package com.webjob.application.dto.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webjob.application.enums.*;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobRequest {

    @NotBlank(message = "Tên công việc không được để trống")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255)
    private String location;

    @NotNull
    @PositiveOrZero
    private Double salaryMin;

    @NotNull
    @PositiveOrZero
    private Double salaryMax;

    private boolean negotiable;

    private String currency;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private JobLevel level;

    private Integer experienceRequired;

    @NotNull
    private WorkingType workingType;

    @NotNull
    private WorkMode workMode;

    private String benefits;

    private String requirement;

    private String responsibility;

    @NotNull
    private CompetitionLevel competitionLevel;


    @NotBlank
    private String description;

    @NotNull

    private Instant startDate;

    @NotNull
    private Instant endDate;

    @NotNull
    private JobStatus status;

    @NotEmpty(message = "Phải chọn ít nhất một kỹ năng")
    private List<JobSkillRequest> skills;

    @NotNull
    private Long jobCategoryId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobSkillRequest{
        @NotNull
        private Long skillId;

        @NotNull
        private Boolean required;

        @Min(1)
        private Integer priority;

        @Min(0)
        private Integer experienceYear;

        @NotNull
        private SkillLevel level;
    }


}
