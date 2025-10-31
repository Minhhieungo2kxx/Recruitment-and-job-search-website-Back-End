package com.webjob.application.Dto.Request.Search;

import com.webjob.application.Model.Enums.CompetitionLevel;
import com.webjob.application.Model.Enums.JobCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobFiltersearch {
    private String name;
    private String location;
    private String level;
    private String description;
    private Double minSalary;
    private Double maxSalary;

    private CompetitionLevel competitionLevel;

    private JobCategory jobCategory;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant endDate;

    private Boolean active;

    private List<Long> skillIds;

    private String page;
    private Integer size = 8;
}
