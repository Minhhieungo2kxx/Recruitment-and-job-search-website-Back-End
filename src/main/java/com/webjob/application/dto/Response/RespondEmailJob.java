package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RespondEmailJob {
    // Thông tin cơ bản
    private String name;
    private String description;

    // Công ty
    private CompanyEmail company;

    // Mức lương
    private String formattedSalary;

    // Địa điểm
    private String location;

    // Loại công việc
    private String workingType;
    private String workMode;

    private String level;

    // Kinh nghiệm
    private Integer experienceRequired;

    // Số lượng tuyển
    private Integer quantity;

    // Danh mục
    private String category;

    // Kỹ năng
    private List<SkillEmail> skills;

    // Thời gian
    private String startDate;
    private String endDate;

    // Nội dung công việc
    private String responsibility;
    private String requirement;
    private String benefits;

    private String competitionLevel;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CompanyEmail {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillEmail {
        private String name;
    }


}
