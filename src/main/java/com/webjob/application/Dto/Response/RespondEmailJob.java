package com.webjob.application.Dto.Response;

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
public class RespondEmailJob {
    private String name;
    private String formattedSalary; // => dùng cho hiển thị
    private CompanyEmail company;
    private List<SkillEmail> skills;
    private String startDate;
    private String endDate;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyEmail{
        private String name;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillEmail{
        private String name;
    }


}
