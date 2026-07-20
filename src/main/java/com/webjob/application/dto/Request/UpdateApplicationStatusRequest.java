package com.webjob.application.dto.Request;

import com.webjob.application.enums.ResumeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateApplicationStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private ResumeStatus status;



}
