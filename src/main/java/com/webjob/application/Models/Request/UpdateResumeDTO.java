package com.webjob.application.Models.Request;

import com.webjob.application.Models.Enums.ResumeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
