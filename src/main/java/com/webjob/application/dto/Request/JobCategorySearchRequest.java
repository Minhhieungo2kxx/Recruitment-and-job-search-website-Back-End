package com.webjob.application.dto.Request;

import com.webjob.application.enums.CategoryStatus;
import lombok.Data;

@Data
public class JobCategorySearchRequest {
    private String keyword;

    private CategoryStatus status;
}
