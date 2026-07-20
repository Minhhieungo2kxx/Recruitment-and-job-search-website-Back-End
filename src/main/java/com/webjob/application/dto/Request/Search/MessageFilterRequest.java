package com.webjob.application.dto.Request.Search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageFilterRequest {
    private String status;
    private String type;
    private Boolean isDeleted;
}
