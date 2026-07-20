package com.webjob.application.dto.Request;

import com.webjob.application.enums.SubscriberStatus;
import com.webjob.application.models.Entity.Skill;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberRequest {
    @NotBlank(message = "Tên không được để trống")
    private String name;



    @Pattern(
            regexp = "^\\+?[0-9]{9,15}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phoneNumber;

    @Size(max = 500)
    private String description;

    private SubscriberStatus status;

    private boolean subscribed ;


    @NotEmpty(message = "Phải chọn ít nhất một skill")
    private List<Long> skillIds;
}
