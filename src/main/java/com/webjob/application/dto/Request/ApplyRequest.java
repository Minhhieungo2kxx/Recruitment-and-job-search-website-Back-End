package com.webjob.application.dto.Request;

import com.webjob.application.annotation.EitherResumeOrPublicId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EitherResumeOrPublicId
@Builder
public class ApplyRequest {


    @NotNull
    private Long jobId;

    private String fullName;

    private String email;

    //chọn CV cũ
    private Long resumeId;

    //upload mới
    private String publicId;

    private Boolean isDefault;
}
