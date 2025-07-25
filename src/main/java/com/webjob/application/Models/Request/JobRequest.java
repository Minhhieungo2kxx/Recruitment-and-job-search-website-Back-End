package com.webjob.application.Models.Request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobRequest {

    @NotBlank(message = "Tên công việc không được để trống")
    @Size(max = 255, message = "Tên công việc không được vượt quá 255 ký tự")
    private String name;


    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String location;


    @NotNull(message = "Mức lương không được để trống")
    @PositiveOrZero(message = "Mức lương phải là số không âm")
    private double salary;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    private int quantity;

    @NotBlank(message = "Cấp độ không được để trống")
    @Pattern(
            regexp = "^(INTERN|FRESHER|JUNIOR|MIDDLE|SENIOR)$",
            message = "Cấp độ không hợp lệ. Giá trị hợp lệ: INTERN, FRESHER, JUNIOR, MIDDLE, SENIOR"
    )
    private String level;

    @Column(columnDefinition = "MEDIUMTEXT")
    @NotBlank(message = "Mô tả công việc không được để trống")
    private String description;

    @NotNull(message = "Thời hạn bắt đầu không được để trống")
    private Instant startDate;

    @NotNull(message = "Thời hạn kết thúc không được để trống")
    private Instant endDate;

    private boolean active;

    @NotNull(message = "Công ty không được để trống")
    private Long companyId; // nếu cần

    @NotEmpty(message = "Danh sách kỹ năng không được rỗng")
    private List<SkillIdDTO> skills;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkillIdDTO{
        private Long id;
    }

}
