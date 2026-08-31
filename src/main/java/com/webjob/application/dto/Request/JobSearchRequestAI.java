package com.webjob.application.dto.Request;

import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.enums.WorkingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSearchRequestAI {
    private String keyword;
    private String location;
    private Double salaryMin;
    private Integer experienceYears;
    private JobLevel level;
    private WorkMode workMode;
    private WorkingType workingType;
    private String companyName;
    private String categoryName;
}
