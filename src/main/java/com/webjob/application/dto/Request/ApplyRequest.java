package com.webjob.application.dto.Request;

import com.webjob.application.annotation.EitherResumeOrPublicId;
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
@EitherResumeOrPublicId
public class ApplyRequest {


    @NotNull
    private Long jobId;


    //chọn CV cũ
    private Long resumeId;

    //upload mới
    private String publicId;

    private Boolean isDefault=false;
}
