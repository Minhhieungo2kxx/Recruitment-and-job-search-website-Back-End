package com.webjob.application.Dto.Request;

import com.webjob.application.Model.Enums.ResumeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateResumeDTO {

    private Long id;

    @NotNull(message = "Trạng thái không được để trống")
    private ResumeStatus status;


}
