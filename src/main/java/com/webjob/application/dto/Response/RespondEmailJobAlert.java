package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RespondEmailJobAlert {
    // Thông tin cơ bản
    private String name;
    private String description;

    private String companyName;


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
    // Thời gian
    private String startDate;
    private String endDate;

    // Nội dung công việc
    private String responsibility;
    private String requirement;
    private String benefits;

    private String competitionLevel;

}
