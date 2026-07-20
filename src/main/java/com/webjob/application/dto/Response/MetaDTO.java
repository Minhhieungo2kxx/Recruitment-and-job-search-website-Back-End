package com.webjob.application.dto.Response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaDTO {
    private int current;
    private int pageSize;
    private int totalPages;
    private long totalItems;
}
