package com.webjob.application.dto.Request;

import com.webjob.application.enums.AlertFrequency;
import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.WorkMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobAlertRequest {
    @Size(max = 100)
    private String keyword;

    @Size(max = 100)
    private String location;

    @Positive
    private Double salaryMin;

    @Positive
    private Double salaryMax;

    private Long jobCategoryId;

    private JobLevel level;

    @NotNull
    private AlertFrequency frequency;

    private WorkMode workMode;
}
