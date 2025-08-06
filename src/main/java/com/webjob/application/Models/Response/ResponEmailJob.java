package com.webjob.application.Models.Response;

import com.webjob.application.Models.Skill;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponEmailJob {
    private String name;
    private String formattedSalary; // => dùng cho hiển thị
    private CompanyEmail company;
    private List<SkillEmail> skills;

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
