package com.webjob.application.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadResumeRequest {
    @NotNull
    private String publicId;
    private Boolean isDefault=false;

}
