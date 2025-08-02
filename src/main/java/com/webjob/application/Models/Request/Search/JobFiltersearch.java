package com.webjob.application.Models.Request.Search;

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant endDate;

    private Boolean active;

    private List<Long> skillIds;

    private String page;
    private Integer size = 8;
}
