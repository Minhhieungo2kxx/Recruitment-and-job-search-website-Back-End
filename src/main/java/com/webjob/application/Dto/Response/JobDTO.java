package com.webjob.application.Dto.Response;

import com.webjob.application.Model.Enums.CompetitionLevel;
import com.webjob.application.Model.Enums.JobCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {
    private Long id;
    private String name;
    private String location;
    private double salary;
    private int quantity;
    private String level;
    private int appliedCount;
    private CompetitionLevel competitionLevel;
    private JobCategory jobCategory;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Company company;
    private List<Skill> skills;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Company {
        private Long id;
        private String name;
        private String description;
        private String address;
        private String logo;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        private Long id;
        private String name;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
    }

}
